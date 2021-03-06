// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <cppunit/extensions/HelperMacros.h>
#include <vespa/metrics/metricmanager.h>
#include <vespa/storageapi/message/bucket.h>
#include <vespa/storageapi/message/state.h>
#include <vespa/vdslib/state/cluster_state_bundle.h>
#include <vespa/storage/frameworkimpl/component/storagecomponentregisterimpl.h>
#include <vespa/storage/storageserver/statemanager.h>
#include <tests/common/teststorageapp.h>
#include <tests/common/testhelper.h>
#include <tests/common/dummystoragelink.h>
#include <vespa/vespalib/data/slime/slime.h>

using storage::lib::NodeState;
using storage::lib::NodeType;
using storage::lib::State;
using storage::lib::ClusterState;

namespace storage {

struct StateManagerTest : public CppUnit::TestFixture {
    std::unique_ptr<TestServiceLayerApp> _node;
    std::unique_ptr<DummyStorageLink> _upper;
    std::unique_ptr<metrics::MetricManager> _metricManager;
    StateManager* _manager;
    DummyStorageLink* _lower;

    StateManagerTest();

    void setUp() override;
    void tearDown() override;

    void testSystemState();
    void testReportedNodeState();
    void current_cluster_state_version_is_included_in_host_info_json();
    void can_explicitly_send_get_node_state_reply();
    void explicit_node_state_replying_without_pending_request_immediately_replies_on_next_request();
    void immediate_node_state_replying_is_tracked_per_controller();

    CPPUNIT_TEST_SUITE(StateManagerTest);
    CPPUNIT_TEST(testSystemState);
    CPPUNIT_TEST(testReportedNodeState);
    CPPUNIT_TEST(current_cluster_state_version_is_included_in_host_info_json);
    CPPUNIT_TEST(can_explicitly_send_get_node_state_reply);
    CPPUNIT_TEST(explicit_node_state_replying_without_pending_request_immediately_replies_on_next_request);
    CPPUNIT_TEST(immediate_node_state_replying_is_tracked_per_controller);
    CPPUNIT_TEST_SUITE_END();

    void mark_reported_node_state_up();
    void send_down_get_node_state_request(uint16_t controller_index);
    void assert_ok_get_node_state_reply_sent_and_clear();
    void clear_sent_replies();
    void mark_reply_observed_from_n_controllers(uint16_t n);
};

CPPUNIT_TEST_SUITE_REGISTRATION(StateManagerTest);

StateManagerTest::StateManagerTest()
    : _node(),
      _upper(),
      _manager(0),
      _lower(0)
{
}

void
StateManagerTest::setUp() {
    try{
        vdstestlib::DirConfig config(getStandardConfig(true));
        _node.reset(new TestServiceLayerApp(DiskCount(1), NodeIndex(2)));
        // Clock will increase 1 sec per call.
        _node->getClock().setAbsoluteTimeInSeconds(1);
        _metricManager.reset(new metrics::MetricManager);
        _upper.reset(new DummyStorageLink());
        _manager = new StateManager(_node->getComponentRegister(),
                                    *_metricManager,
                                    std::unique_ptr<HostInfo>(new HostInfo));
        _lower = new DummyStorageLink();
        _upper->push_back(StorageLink::UP(_manager));
        _upper->push_back(StorageLink::UP(_lower));
        _upper->open();
    } catch (std::exception& e) {
        std::cerr << "Failed to static initialize objects: " << e.what()
                  << "\n";
    }
}

void
StateManagerTest::tearDown() {
    CPPUNIT_ASSERT_EQUAL(size_t(0), _lower->getNumReplies());
    CPPUNIT_ASSERT_EQUAL(size_t(0), _lower->getNumCommands());
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumReplies());
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumCommands());
    _manager = 0;
    _lower = 0;
    _upper->close();
    _upper->flush();
    _upper.reset(0);
    _node.reset(0);
    _metricManager.reset();
}

#define GET_ONLY_OK_REPLY(varname) \
{ \
    CPPUNIT_ASSERT_EQUAL(size_t(1), _upper->getNumReplies()); \
    CPPUNIT_ASSERT(_upper->getReply(0)->getType().isReply()); \
    varname = std::dynamic_pointer_cast<api::StorageReply>( \
                    _upper->getReply(0)); \
    CPPUNIT_ASSERT(varname != 0); \
    _upper->reset(); \
    CPPUNIT_ASSERT_EQUAL(api::ReturnCode(api::ReturnCode::OK), \
                         varname->getResult()); \
}

void
StateManagerTest::testSystemState()
{
    std::shared_ptr<api::StorageReply> reply;
        // Verify initial state on startup
    ClusterState::CSP currentState = _manager->getClusterStateBundle()->getBaselineClusterState();
    CPPUNIT_ASSERT_EQUAL(std::string("cluster:d"),
                         currentState->toString(false));

    NodeState::CSP currentNodeState = _manager->getCurrentNodeState();
    CPPUNIT_ASSERT_EQUAL(std::string("s:d"), currentNodeState->toString(false));

    ClusterState sendState("storage:4 .2.s:m");
    std::shared_ptr<api::SetSystemStateCommand> cmd(
            new api::SetSystemStateCommand(sendState));
    _upper->sendDown(cmd);
    GET_ONLY_OK_REPLY(reply);

    currentState = _manager->getClusterStateBundle()->getBaselineClusterState();
    CPPUNIT_ASSERT_EQUAL(sendState, *currentState);

    currentNodeState = _manager->getCurrentNodeState();
    CPPUNIT_ASSERT_EQUAL(std::string("s:m"), currentNodeState->toString(false));
}

namespace {
    struct MyStateListener : public StateListener {
        const NodeStateUpdater& updater;
        lib::NodeState current;
        std::ostringstream ost;

        MyStateListener(const NodeStateUpdater& upd)
            : updater(upd), current(*updater.getReportedNodeState()) {}
        ~MyStateListener() { }

        void handleNewState() override {
            ost << current << " -> ";
            current = *updater.getReportedNodeState();
            ost << current << "\n";
        }
    };
}

void
StateManagerTest::testReportedNodeState()
{
    std::shared_ptr<api::StorageReply> reply;
        // Add a state listener to check that we get events.
    MyStateListener stateListener(*_manager);
    _manager->addStateListener(stateListener);
        // Test that initial state is initializing
    NodeState::CSP nodeState = _manager->getReportedNodeState();
    CPPUNIT_ASSERT_EQUAL(std::string("s:i b:58 i:0 t:1"), nodeState->toString(false));
        // Test that it works to update the state
    {
        NodeStateUpdater::Lock::SP lock(_manager->grabStateChangeLock());
        NodeState ns(*_manager->getReportedNodeState());
        ns.setState(State::UP);
        _manager->setReportedNodeState(ns);
    }
        // And that we get the change both through state interface
    nodeState = _manager->getReportedNodeState();
    CPPUNIT_ASSERT_EQUAL(std::string("s:u b:58 t:1"),
                         nodeState->toString(false));
        // And get node state command (no expected state)
    std::shared_ptr<api::GetNodeStateCommand> cmd(
            new api::GetNodeStateCommand(lib::NodeState::UP()));
    _upper->sendDown(cmd);
    GET_ONLY_OK_REPLY(reply);
    CPPUNIT_ASSERT_EQUAL(api::MessageType::GETNODESTATE_REPLY,
                         reply->getType());
    nodeState.reset(new NodeState(
                dynamic_cast<api::GetNodeStateReply&>(*reply).getNodeState()));
    CPPUNIT_ASSERT_EQUAL(std::string("s:u b:58 t:1"),
                         nodeState->toString(false));
        // We should also get it with wrong expected state
    cmd.reset(new api::GetNodeStateCommand(lib::NodeState::UP(new NodeState(NodeType::STORAGE, State::INITIALIZING))));
    _upper->sendDown(cmd);
    GET_ONLY_OK_REPLY(reply);
    CPPUNIT_ASSERT_EQUAL(api::MessageType::GETNODESTATE_REPLY,
                         reply->getType());
    nodeState.reset(new NodeState(
                dynamic_cast<api::GetNodeStateReply&>(*reply).getNodeState()));
    CPPUNIT_ASSERT_EQUAL(std::string("s:u b:58 t:1"),
                         nodeState->toString(false));
        // With correct wanted state we should not get response right away
    cmd.reset(new api::GetNodeStateCommand(
                      lib::NodeState::UP(new NodeState("s:u b:58 t:1", &NodeType::STORAGE))));
    _upper->sendDown(cmd);
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumReplies());
        // But when we update state, we get the reply
    {
        NodeStateUpdater::Lock::SP lock(_manager->grabStateChangeLock());
        NodeState ns(*_manager->getReportedNodeState());
        ns.setState(State::STOPPING);
        ns.setDescription("Stopping node");
        _manager->setReportedNodeState(ns);
    }

    GET_ONLY_OK_REPLY(reply);
    CPPUNIT_ASSERT_EQUAL(api::MessageType::GETNODESTATE_REPLY,
                         reply->getType());
    nodeState.reset(new NodeState(
                dynamic_cast<api::GetNodeStateReply&>(*reply).getNodeState()));
    CPPUNIT_ASSERT_EQUAL(std::string("s:s b:58 t:1 m:Stopping\\x20node"),
                         nodeState->toString(false));

        // Removing state listener, it stops getting updates
    _manager->removeStateListener(stateListener);
        // Do another update which listener should not get..
    {
        NodeStateUpdater::Lock::SP lock(_manager->grabStateChangeLock());
        NodeState ns(*_manager->getReportedNodeState());
        ns.setState(State::UP);
        _manager->setReportedNodeState(ns);
    }
    std::string expectedEvents =
            "s:i b:58 i:0 t:1 -> s:u b:58 t:1\n"
            "s:u b:58 t:1 -> s:s b:58 t:1 m:Stopping\\x20node\n";
    CPPUNIT_ASSERT_EQUAL(expectedEvents, stateListener.ost.str());
}

void StateManagerTest::current_cluster_state_version_is_included_in_host_info_json() {
    ClusterState state(*_manager->getClusterStateBundle()->getBaselineClusterState());
    state.setVersion(123);
    _manager->setClusterStateBundle(lib::ClusterStateBundle(state));

    std::string nodeInfoString(_manager->getNodeInfo());
    vespalib::Memory goldenMemory(nodeInfoString);
    vespalib::Slime nodeInfo;
    vespalib::slime::JsonFormat::decode(nodeInfoString, nodeInfo);
    
    vespalib::slime::Symbol lookupSymbol =
        nodeInfo.lookup("cluster-state-version");
    if (lookupSymbol.undefined()) {
        CPPUNIT_FAIL("No cluster-state-version was found in the node info");
    }

    auto& cursor = nodeInfo.get();
    auto& clusterStateVersionCursor = cursor["cluster-state-version"];
    if (!clusterStateVersionCursor.valid()) {
        CPPUNIT_FAIL("No cluster-state-version was found in the node info");
    }

    if (clusterStateVersionCursor.type().getId() != vespalib::slime::LONG::ID) {
        CPPUNIT_FAIL("No cluster-state-version was found in the node info");
    }

    int version = clusterStateVersionCursor.asLong();
    CPPUNIT_ASSERT_EQUAL(123, version);
}

void StateManagerTest::mark_reported_node_state_up() {
    auto lock = _manager->grabStateChangeLock();
    _manager->setReportedNodeState(NodeState(NodeType::STORAGE, State::UP));
}

void StateManagerTest::send_down_get_node_state_request(uint16_t controller_index) {
    auto cmd = std::make_shared<api::GetNodeStateCommand>(
            std::make_unique<NodeState>(NodeType::STORAGE, State::UP));
    cmd->setTimeout(10000000);
    cmd->setSourceIndex(controller_index);
    _upper->sendDown(cmd);
}

void StateManagerTest::assert_ok_get_node_state_reply_sent_and_clear() {
    CPPUNIT_ASSERT_EQUAL(size_t(1), _upper->getNumReplies());
    std::shared_ptr<api::StorageReply> reply;
    GET_ONLY_OK_REPLY(reply); // Implicitly clears messages from _upper
    CPPUNIT_ASSERT_EQUAL(api::MessageType::GETNODESTATE_REPLY, reply->getType());
}

void StateManagerTest::clear_sent_replies() {
    _upper->getRepliesOnce();
}

void StateManagerTest::mark_reply_observed_from_n_controllers(uint16_t n) {
    for (uint16_t i = 0; i < n; ++i) {
        send_down_get_node_state_request(i);
        assert_ok_get_node_state_reply_sent_and_clear();
    }
}

void StateManagerTest::can_explicitly_send_get_node_state_reply() {
    mark_reported_node_state_up();
    // Must "pre-trigger" that a controller has already received a GetNodeState
    // reply, or an immediate reply will be sent by default when the first request
    // from a controller is observed.
    mark_reply_observed_from_n_controllers(1);

    send_down_get_node_state_request(0);
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumReplies());

    _manager->immediately_send_get_node_state_replies();
    assert_ok_get_node_state_reply_sent_and_clear();
}

void StateManagerTest::explicit_node_state_replying_without_pending_request_immediately_replies_on_next_request() {
    mark_reported_node_state_up();
    mark_reply_observed_from_n_controllers(1);

    // No pending requests at this time
    _manager->immediately_send_get_node_state_replies();

    send_down_get_node_state_request(0);
    assert_ok_get_node_state_reply_sent_and_clear();
    // Sending a new request should now _not_ immediately receive a reply
    send_down_get_node_state_request(0);
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumReplies());
}

void StateManagerTest::immediate_node_state_replying_is_tracked_per_controller() {
    mark_reported_node_state_up();
    mark_reply_observed_from_n_controllers(3);

    _manager->immediately_send_get_node_state_replies();

    send_down_get_node_state_request(0);
    send_down_get_node_state_request(1);
    send_down_get_node_state_request(2);
    CPPUNIT_ASSERT_EQUAL(size_t(3), _upper->getNumReplies());
    clear_sent_replies();

    // Sending a new request should now _not_ immediately receive a reply
    send_down_get_node_state_request(0);
    send_down_get_node_state_request(1);
    send_down_get_node_state_request(2);
    CPPUNIT_ASSERT_EQUAL(size_t(0), _upper->getNumReplies());
}

} // storage
