// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.tensor.functions;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory of scalar Java functions.
 * The purpose of this is to embellish anonymous functions with a runtime type
 * such that they can be inspected and will return a parsable toString.
 *
 * @author bratseth
 */
public class ScalarFunctions {

    public static DoubleBinaryOperator add() { return new Add(); }
    public static DoubleBinaryOperator divide() { return new Divide(); }
    public static DoubleBinaryOperator equal() { return new Equal(); }
    public static DoubleBinaryOperator greater() { return new Greater(); }
    public static DoubleBinaryOperator less() { return new Less(); }
    public static DoubleBinaryOperator max() { return new Max(); }
    public static DoubleBinaryOperator min() { return new Min(); }
    public static DoubleBinaryOperator mean() { return new Mean(); }
    public static DoubleBinaryOperator multiply() { return new Multiply(); }
    public static DoubleBinaryOperator pow() { return new Pow(); }
    public static DoubleBinaryOperator squareddifference() { return new SquaredDifference(); }
    public static DoubleBinaryOperator subtract() { return new Subtract(); }

    public static DoubleUnaryOperator abs() { return new Abs(); }
    public static DoubleUnaryOperator acos() { return new Acos(); }
    public static DoubleUnaryOperator asin() { return new Asin(); }
    public static DoubleUnaryOperator atan() { return new Atan(); }
    public static DoubleUnaryOperator ceil() { return new Ceil(); }
    public static DoubleUnaryOperator cos() { return new Cos(); }
    public static DoubleUnaryOperator elu() { return new Elu(); }
    public static DoubleUnaryOperator exp() { return new Exp(); }
    public static DoubleUnaryOperator floor() { return new Floor(); }
    public static DoubleUnaryOperator log() { return new Log(); }
    public static DoubleUnaryOperator neg() { return new Neg(); }
    public static DoubleUnaryOperator reciprocal() { return new Reciprocal(); }
    public static DoubleUnaryOperator relu() { return new Relu(); }
    public static DoubleUnaryOperator rsqrt() { return new Rsqrt(); }
    public static DoubleUnaryOperator selu() { return new Selu(); }
    public static DoubleUnaryOperator sin() { return new Sin(); }
    public static DoubleUnaryOperator sigmoid() { return new Sigmoid(); }
    public static DoubleUnaryOperator sqrt() { return new Sqrt(); }
    public static DoubleUnaryOperator square() { return new Square(); }
    public static DoubleUnaryOperator tan() { return new Tan(); }
    public static DoubleUnaryOperator tanh() { return new Tanh(); }

    public static Function<List<Long>, Double> random() { return new Random(); }
    public static Function<List<Long>, Double> equal(List<String> argumentNames) { return new EqualElements(argumentNames); }
    public static Function<List<Long>, Double> sum(List<String> argumentNames) { return new SumElements(argumentNames); }

    // Binary operators -----------------------------------------------------------------------------

    public static class Add implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left + right; }
        @Override
        public String toString() { return "f(a,b)(a + b)"; }
    }

    public static class Equal implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left == right ? 1 : 0; }
        @Override
        public String toString() { return "f(a,b)(a==b)"; }
    }

    public static class Greater implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left > right ? 1 : 0; }
        @Override
        public String toString() { return "f(a,b)(a > b)"; }
    }

    public static class Less implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left < right ? 1 : 0; }
        @Override
        public String toString() { return "f(a,b)(a < b)"; }
    }

    public static class Max implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return Math.max(left, right); }
        @Override
        public String toString() { return "f(a,b)(max(a, b))"; }
    }

    public static class Min implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return Math.min(left, right); }
        @Override
        public String toString() { return "f(a,b)(min(a, b))"; }
    }

    public static class Mean implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return (left + right) / 2; }
        @Override
        public String toString() { return "f(a,b)((a + b) / 2)"; }
    }

    public static class Multiply implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left * right; }
        @Override
        public String toString() { return "f(a,b)(a * b)"; }
    }

    public static class Pow implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return Math.pow(left, right); }
        @Override
        public String toString() { return "f(a,b)(pow(a, b))"; }
    }

    public static class Divide implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left / right; }
        @Override
        public String toString() { return "f(a,b)(a / b)"; }
    }

    public static class SquaredDifference implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return (left - right) * (left - right); }
        @Override
        public String toString() { return "f(a,b)((a-b) * (a-b))"; }
    }

    public static class Subtract implements DoubleBinaryOperator {
        @Override
        public double applyAsDouble(double left, double right) { return left - right; }
        @Override
        public String toString() { return "f(a,b)(a - b)"; }
    }


    // Unary operators ------------------------------------------------------------------------------

    public static class Abs implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.abs(operand); }
        @Override
        public String toString() { return "f(a)(fabs(a))"; }
    }

    public static class Acos implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.acos(operand); }
        @Override
        public String toString() { return "f(a)(acos(a))"; }
    }

    public static class Asin implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.asin(operand); }
        @Override
        public String toString() { return "f(a)(asin(a))"; }
    }

    public static class Atan implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.atan(operand); }
        @Override
        public String toString() { return "f(a)(atan(a))"; }
    }

    public static class Ceil implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.ceil(operand); }
        @Override
        public String toString() { return "f(a)(ceil(a))"; }
    }

    public static class Cos implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.cos(operand); }
        @Override
        public String toString() { return "f(a)(cos(a))"; }
    }

    public static class Elu implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return operand < 0 ? Math.exp(operand) -1 : operand; }
        @Override
        public String toString() { return "f(a)(if(a < 0, exp(a)-1, a))"; }
    }

    public static class Exp implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.exp(operand); }
        @Override
        public String toString() { return "f(a)(exp(a))"; }
    }

    public static class Floor implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.floor(operand); }
        @Override
        public String toString() { return "f(a)(floor(a))"; }
    }

    public static class Log implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.log(operand); }
        @Override
        public String toString() { return "f(a)(log(a))"; }
    }

    public static class Neg implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return -operand; }
        @Override
        public String toString() { return "f(a)(-a)"; }
    }

    public static class Reciprocal implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return 1.0 / operand; }
        @Override
        public String toString() { return "f(a)(1 / a)"; }
    }

    public static class Relu implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.max(operand, 0); }
        @Override
        public String toString() { return "f(a)(max(0, a))"; }
    }

    public static class Selu implements DoubleUnaryOperator {
        // See https://arxiv.org/abs/1706.02515
        private static final double scale = 1.0507009873554804934193349852946;
        private static final double alpha = 1.6732632423543772848170429916717;
        @Override
        public double applyAsDouble(double operand) { return scale * (operand >= 0.0 ? operand : alpha * (Math.exp(operand)-1)); }
        @Override
        public String toString() { return "f(a)(" + scale + " * if(a >= 0, a, " + alpha + " * (exp(a) - 1)))"; }
    }

    public static class Sin implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.sin(operand); }
        @Override
        public String toString() { return "f(a)(sin(a))"; }
    }

    public static class Rsqrt implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return 1.0 / Math.sqrt(operand); }
        @Override
        public String toString() { return "f(a)(1.0 / sqrt(a))"; }
    }

    public static class Sigmoid implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return 1.0 / (1.0 + Math.exp(-operand)); }
        @Override
        public String toString() { return "f(a)(1 / (1 + exp(-a)))"; }
    }

    public static class Sqrt implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.sqrt(operand); }
        @Override
        public String toString() { return "f(a)(sqrt(a))"; }
    }

    public static class Square implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return operand * operand; }
        @Override
        public String toString() { return "f(a)(a * a)"; }
    }

    public static class Tan implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.tan(operand); }
        @Override
        public String toString() { return "f(a)(tan(a))"; }
    }

    public static class Tanh implements DoubleUnaryOperator {
        @Override
        public double applyAsDouble(double operand) { return Math.tanh(operand); }
        @Override
        public String toString() { return "f(a)(tanh(a))"; }
    }


    // Variable-length operators -----------------------------------------------------------------------------

    public static class EqualElements implements Function<List<Long>, Double> {
        private final ImmutableList<String> argumentNames;
        private EqualElements(List<String> argumentNames) {
            this.argumentNames = ImmutableList.copyOf(argumentNames);
        }

        @Override
        public Double apply(List<Long> values) {
            if (values.isEmpty()) return 1.0;
            for (Long value : values)
                if ( ! value.equals(values.get(0)))
                    return 0.0;
            return 1.0;
        }
        @Override
        public String toString() {
            if (argumentNames.size() == 0) return "1";
            if (argumentNames.size() == 1) return "1";
            if (argumentNames.size() == 2) return argumentNames.get(0) + "==" + argumentNames.get(1);

            StringBuilder b = new StringBuilder();
            for (int i = 0; i < argumentNames.size() -1; i++) {
                b.append("(").append(argumentNames.get(i)).append("==").append(argumentNames.get(i+1)).append(")");
                if ( i < argumentNames.size() -2)
                    b.append("*");
            }
            return b.toString();
        }
    }

    public static class Random implements Function<List<Long>, Double> {
        @Override
        public Double apply(List<Long> values) {
            return ThreadLocalRandom.current().nextDouble();
        }
        @Override
        public String toString() { return "random"; }
    }

    public static class SumElements implements Function<List<Long>, Double> {
        private final ImmutableList<String> argumentNames;
        private SumElements(List<String> argumentNames) {
            this.argumentNames = ImmutableList.copyOf(argumentNames);
        }

        @Override
        public Double apply(List<Long> values) {
            long sum = 0;
            for (Long value : values)
                sum += value;
            return (double)sum;
        }
        @Override
        public String toString() {
            return argumentNames.stream().collect(Collectors.joining("+"));
        }
    }

}
