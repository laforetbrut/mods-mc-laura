package com.vyrriox.lauramod.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A small Molang interpreter, enough for Blockbench animations: numbers, arithmetic, comparisons,
 * ternaries, {@code query.*} values and the common {@code math.*} functions. Angles in trigonometric
 * functions are in degrees, like in Bedrock.
 *
 * @author vyrriox
 */
public final class Molang {
    /** Values an expression can read. */
    public static final class Context {
        public double animTime;
        public double lifeTime;
        public double groundSpeed;
        public double distanceMoved;
        public double health = 20;
        public double maxHealth = 20;
        public double headYaw;
        public double headPitch;
        public double isOnGround = 1;
        public double isInWater;
        public double isSitting;
        public double isSleeping;
    }

    /** A parsed expression. */
    public interface Expr {
        double eval(Context ctx);

        default boolean isConstant() {
            return false;
        }
    }

    public record Constant(double value) implements Expr {
        @Override
        public double eval(Context ctx) {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }

    public static final Expr ZERO = new Constant(0);
    public static final Expr ONE = new Constant(1);

    /** Longest expression read. Animation values are a few dozen characters. */
    static final int MAX_LENGTH = 4096;
    /** Deepest nesting of parentheses, function calls, ternaries and signs. */
    static final int MAX_DEPTH = 48;
    /** Most operators and function calls in one expression (also the deepest tree to evaluate). */
    static final int MAX_NODES = 512;

    private Molang() {
    }

    /**
     * Parses an expression; invalid input becomes 0 rather than breaking the model. The limits
     * above bound the recursion of the parser and of {@link Expr#eval}, so a file made to nest
     * thousands of levels is read as 0 instead of overflowing the stack.
     */
    public static Expr parse(String source) {
        if (source == null) {
            return ZERO;
        }
        String s = source.trim();
        if (s.isEmpty() || s.length() > MAX_LENGTH) {
            return ZERO;
        }
        try {
            return new Constant(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {
            // Not a plain number.
        }
        s = s.toLowerCase(Locale.ROOT);
        int ret = s.lastIndexOf("return ");
        if (ret >= 0) {
            s = s.substring(ret + 7);
        }
        s = s.replace(";", "").trim();
        try {
            Parser parser = new Parser(s);
            Expr expr = parser.parseTernary();
            if (parser.pos < parser.src.length()) {
                return ZERO;
            }
            return fold(expr);
        } catch (RuntimeException e) {
            return ZERO;
        }
    }

    private static Expr fold(Expr expr) {
        if (expr instanceof Constant) {
            return expr;
        }
        if (expr instanceof Node node && node.allConstant()) {
            return new Constant(expr.eval(new Context()));
        }
        return expr;
    }

    /** A non-constant expression built from children. */
    private interface Node extends Expr {
        boolean allConstant();
    }

    private record Binary(char op, Expr a, Expr b) implements Node {
        @Override
        public double eval(Context c) {
            double x = a.eval(c);
            double y = b.eval(c);
            return switch (op) {
                case '+' -> x + y;
                case '-' -> x - y;
                case '*' -> x * y;
                case '/' -> y == 0 ? 0 : x / y;
                case '%' -> y == 0 ? 0 : x % y;
                case '<' -> x < y ? 1 : 0;
                case '>' -> x > y ? 1 : 0;
                case 'l' -> x <= y ? 1 : 0;
                case 'g' -> x >= y ? 1 : 0;
                case '=' -> x == y ? 1 : 0;
                case '!' -> x != y ? 1 : 0;
                case '&' -> x != 0 && y != 0 ? 1 : 0;
                case '|' -> x != 0 || y != 0 ? 1 : 0;
                case '?' -> x != 0 ? x : y;
                default -> 0;
            };
        }

        @Override
        public boolean allConstant() {
            return a.isConstant() && b.isConstant();
        }
    }

    private record Negate(Expr a) implements Node {
        @Override
        public double eval(Context c) {
            return -a.eval(c);
        }

        @Override
        public boolean allConstant() {
            return a.isConstant();
        }
    }

    private record Not(Expr a) implements Node {
        @Override
        public double eval(Context c) {
            return a.eval(c) == 0 ? 1 : 0;
        }

        @Override
        public boolean allConstant() {
            return a.isConstant();
        }
    }

    private record Ternary(Expr cond, Expr yes, Expr no) implements Node {
        @Override
        public double eval(Context c) {
            return cond.eval(c) != 0 ? yes.eval(c) : no.eval(c);
        }

        @Override
        public boolean allConstant() {
            return cond.isConstant() && yes.isConstant() && no.isConstant();
        }
    }

    private record Variable(String name) implements Expr {
        @Override
        public double eval(Context c) {
            return switch (name) {
                case "anim_time" -> c.animTime;
                case "life_time" -> c.lifeTime;
                case "ground_speed", "modified_move_speed" -> c.groundSpeed;
                case "modified_distance_moved", "distance_moved" -> c.distanceMoved;
                case "health" -> c.health;
                case "max_health" -> c.maxHealth;
                case "head_y_rotation" -> c.headYaw;
                case "head_x_rotation" -> c.headPitch;
                case "is_on_ground" -> c.isOnGround;
                case "is_in_water" -> c.isInWater;
                case "is_sitting" -> c.isSitting;
                case "is_sleeping" -> c.isSleeping;
                default -> 0;
            };
        }
    }

    private record Function(String name, List<Expr> args) implements Node {
        @Override
        public double eval(Context c) {
            double a = args.isEmpty() ? 0 : args.get(0).eval(c);
            double b = args.size() > 1 ? args.get(1).eval(c) : 0;
            double d = args.size() > 2 ? args.get(2).eval(c) : 0;
            return switch (name) {
                case "sin" -> Math.sin(Math.toRadians(a));
                case "cos" -> Math.cos(Math.toRadians(a));
                case "abs" -> Math.abs(a);
                case "sqrt" -> Math.sqrt(Math.max(0, a));
                case "floor" -> Math.floor(a);
                case "ceil" -> Math.ceil(a);
                case "round" -> Math.round(a);
                case "trunc" -> a < 0 ? Math.ceil(a) : Math.floor(a);
                case "exp" -> Math.exp(a);
                case "ln" -> a <= 0 ? 0 : Math.log(a);
                case "pow" -> Math.pow(a, b);
                case "mod" -> b == 0 ? 0 : a % b;
                case "min" -> Math.min(a, b);
                case "max" -> Math.max(a, b);
                case "clamp" -> Math.max(b, Math.min(d, a));
                case "lerp" -> a + (b - a) * d;
                case "lerprotate" -> a + wrap(b - a) * d;
                case "hermite_blend" -> 3 * a * a - 2 * a * a * a;
                case "atan" -> Math.toDegrees(Math.atan(a));
                case "atan2" -> Math.toDegrees(Math.atan2(a, b));
                case "asin" -> Math.toDegrees(Math.asin(a));
                case "acos" -> Math.toDegrees(Math.acos(a));
                case "sign" -> Math.signum(a);
                case "random" -> args.size() >= 2 ? a + ThreadLocalRandom.current().nextDouble() * (b - a) : ThreadLocalRandom.current().nextDouble();
                case "random_integer" -> Math.floor(a + ThreadLocalRandom.current().nextDouble() * (b - a + 1));
                case "die_roll" -> {
                    double sum = 0;
                    for (int i = 0; i < (int) a; i++) {
                        sum += b + ThreadLocalRandom.current().nextDouble() * (d - b);
                    }
                    yield sum;
                }
                default -> 0;
            };
        }

        private static double wrap(double deg) {
            double r = deg % 360;
            if (r >= 180) {
                r -= 360;
            }
            if (r < -180) {
                r += 360;
            }
            return r;
        }

        @Override
        public boolean allConstant() {
            if (name.startsWith("random") || name.equals("die_roll")) {
                return false;
            }
            for (Expr e : args) {
                if (!e.isConstant()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Recursive descent parser, lowest precedence first. */
    private static final class Parser {
        final String src;
        int pos;

        private int depth;
        private int nodes;

        Parser(String src) {
            this.src = src;
        }

        /** One more level of recursion (left again by {@link #leave}). */
        private void enter() {
            if (++depth > MAX_DEPTH) {
                throw new IllegalArgumentException("Expression nested too deep");
            }
        }

        private void leave() {
            depth--;
        }

        /** Counts an operator or a function call. */
        private <T extends Expr> T node(T expr) {
            if (++nodes > MAX_NODES) {
                throw new IllegalArgumentException("Expression too long");
            }
            return expr;
        }

        char peek() {
            skipSpaces();
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        void skipSpaces() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        boolean eat(String token) {
            skipSpaces();
            if (src.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        Expr parseTernary() {
            enter();
            try {
                Expr cond = parseCoalesce();
                if (peek() == '?' && !src.startsWith("??", pos)) {
                    pos++;
                    Expr yes = parseTernary();
                    Expr no = eat(":") ? parseTernary() : ZERO;
                    return node(new Ternary(cond, yes, no));
                }
                return cond;
            } finally {
                leave();
            }
        }

        Expr parseCoalesce() {
            Expr left = parseOr();
            while (eat("??")) {
                left = node(new Binary('?', left, parseOr()));
            }
            return left;
        }

        Expr parseOr() {
            Expr left = parseAnd();
            while (eat("||")) {
                left = node(new Binary('|', left, parseAnd()));
            }
            return left;
        }

        Expr parseAnd() {
            Expr left = parseEquality();
            while (eat("&&")) {
                left = node(new Binary('&', left, parseEquality()));
            }
            return left;
        }

        Expr parseEquality() {
            Expr left = parseCompare();
            while (true) {
                if (eat("==")) {
                    left = node(new Binary('=', left, parseCompare()));
                } else if (eat("!=")) {
                    left = node(new Binary('!', left, parseCompare()));
                } else {
                    return left;
                }
            }
        }

        Expr parseCompare() {
            Expr left = parseAdd();
            while (true) {
                if (eat("<=")) {
                    left = node(new Binary('l', left, parseAdd()));
                } else if (eat(">=")) {
                    left = node(new Binary('g', left, parseAdd()));
                } else if (eat("<")) {
                    left = node(new Binary('<', left, parseAdd()));
                } else if (eat(">")) {
                    left = node(new Binary('>', left, parseAdd()));
                } else {
                    return left;
                }
            }
        }

        Expr parseAdd() {
            Expr left = parseMul();
            while (true) {
                char c = peek();
                if (c == '+' || c == '-') {
                    pos++;
                    left = node(new Binary(c, left, parseMul()));
                } else {
                    return left;
                }
            }
        }

        Expr parseMul() {
            Expr left = parseUnary();
            while (true) {
                char c = peek();
                if (c == '*' || c == '/' || c == '%') {
                    pos++;
                    left = node(new Binary(c, left, parseUnary()));
                } else {
                    return left;
                }
            }
        }

        Expr parseUnary() {
            char c = peek();
            if (c != '-' && c != '+' && c != '!') {
                return parsePrimary();
            }
            pos++;
            enter();
            try {
                Expr operand = parseUnary();
                return c == '-' ? node(new Negate(operand)) : c == '!' ? node(new Not(operand)) : operand;
            } finally {
                leave();
            }
        }

        Expr parsePrimary() {
            char c = peek();
            if (c == '(') {
                pos++;
                Expr inner = parseTernary();
                eat(")");
                return inner;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                    pos++;
                }
                return new Constant(Double.parseDouble(src.substring(start, pos)));
            }
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_' || src.charAt(pos) == '.')) {
                    pos++;
                }
                String ident = src.substring(start, pos);
                if (peek() == '(') {
                    pos++;
                    List<Expr> args = new ArrayList<>();
                    if (peek() != ')') {
                        do {
                            args.add(parseTernary());
                        } while (eat(","));
                    }
                    eat(")");
                    String fn = ident.startsWith("math.") ? ident.substring(5) : ident;
                    return node(new Function(fn, args));
                }
                return identifier(ident);
            }
            throw new IllegalArgumentException("Unexpected character at " + pos);
        }

        private Expr identifier(String ident) {
            if (ident.equals("math.pi")) {
                return new Constant(Math.PI);
            }
            if (ident.equals("true")) {
                return ONE;
            }
            if (ident.equals("false")) {
                return ZERO;
            }
            int dot = ident.indexOf('.');
            String prefix = dot < 0 ? ident : ident.substring(0, dot);
            String name = dot < 0 ? ident : ident.substring(dot + 1);
            if (prefix.equals("query") || prefix.equals("q")) {
                return new Variable(name);
            }
            // variable.*, temp.*, context.*: not supported, read as 0.
            return ZERO;
        }
    }
}
