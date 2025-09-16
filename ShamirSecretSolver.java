import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

/**
 * Shamir's Secret Sharing Polynomial Solver
 * Finds the constant term (secret) from JSON input with encoded shares
 * No external dependencies required
 */
public class ShamirSecretSolver {

    // Point class to hold (x, y) pairs
    static class Point {
        BigInteger x, y;
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    // Result class to hold the secret and details
    static class SolutionResult {
        BigInteger secret;
        String details;
        SolutionResult(BigInteger secret, String details) {
            this.secret = secret;
            this.details = details;
        }
    }

    // Simple JSON parser for our specific format (no external dependencies)
    static class SimpleJsonParser {
        
        public static Map<String, Object> parseJson(String json) {
            Map<String, Object> result = new HashMap<>();
            
            // Clean JSON string
            json = json.replaceAll("\\s+", "");
            
            // Extract keys section
            Pattern keysPattern = Pattern.compile("\"keys\":\\{\"n\":(\\d+),\"k\":(\\d+)\\}");
            Matcher keysMatcher = keysPattern.matcher(json);
            if (keysMatcher.find()) {
                Map<String, Integer> keys = new HashMap<>();
                keys.put("n", Integer.parseInt(keysMatcher.group(1)));
                keys.put("k", Integer.parseInt(keysMatcher.group(2)));
                result.put("keys", keys);
            }
            
            // Extract share points
            Pattern sharePattern = Pattern.compile("\"(\\d+)\":\\{\"base\":\"(\\d+)\",\"value\":\"([^\"]+)\"\\}");
            Matcher shareMatcher = sharePattern.matcher(json);
            
            while (shareMatcher.find()) {
                String shareId = shareMatcher.group(1);
                Map<String, String> share = new HashMap<>();
                share.put("base", shareMatcher.group(2));
                share.put("value", shareMatcher.group(3));
                result.put(shareId, share);
            }
            
            return result;
        }
    }

    // Converts a string value in any base (2-36) to BigInteger
    public static BigInteger parseCustomBase(String value, int base) {
        if (base <= 36) {
            return new BigInteger(value, base);
        }
        // For bases > 36, implement custom parsing
        String digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        BigInteger result = BigInteger.ZERO;
        BigInteger baseBI = BigInteger.valueOf(base);
        String valueStr = value.toLowerCase();
        
        for (int i = 0; i < valueStr.length(); i++) {
            char c = valueStr.charAt(i);
            int digit = digits.indexOf(c);
            if (digit == -1 || digit >= base) {
                throw new IllegalArgumentException("Invalid digit '" + c + "' for base " + base);
            }
            result = result.multiply(baseBI).add(BigInteger.valueOf(digit));
        }
        return result;
    }

    // Parses JSON input format and returns list of points
    @SuppressWarnings("unchecked")
    public static List<Point> parseJson(String jsonInput) {
        List<Point> points = new ArrayList<>();
        Map<String, Object> obj = SimpleJsonParser.parseJson(jsonInput);
        Map<String, Integer> keys = (Map<String, Integer>) obj.get("keys");
        int n = keys.get("n");
        int k = keys.get("k");
        
        System.out.println("📊 Problem Parameters:");
        System.out.println("   Total shares (n): " + n);
        System.out.println("   Required shares (k): " + k);
        System.out.println("   Polynomial degree: " + (k-1));

        System.out.println("\n🔢 Base Conversions:");
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (key.equals("keys")) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, String> share = (Map<String, String>) entry.getValue();
            int base = Integer.parseInt(share.get("base"));
            String yStr = share.get("value");
            BigInteger x = new BigInteger(key);
            BigInteger y = parseCustomBase(yStr, base);
            
            System.out.println("   x=" + x + ": base" + base + "(\"" + yStr + "\") = " + y);
            points.add(new Point(x, y));
        }
        
        // Sort points by x value for consistency
        points.sort(Comparator.comparing(p -> p.x));
        
        // Use exactly k points (minimum required)
        if (points.size() > k) {
            System.out.println("\n⚠️  Using first " + k + " points out of " + points.size() + " available");
            points = points.subList(0, k);
        }
        
        return points;
    }

    // Enhanced Lagrange interpolation with exact rational arithmetic
    public static BigInteger lagrangeInterpolation(List<Point> points) {
        System.out.println("\n🔍 Using Lagrange Interpolation:");
        
        // Use rational arithmetic to avoid precision issues
        BigInteger resultNumerator = BigInteger.ZERO;
        BigInteger resultDenominator = BigInteger.ONE;
        int size = points.size();
        
        for (int i = 0; i < size; i++) {
            BigInteger xi = points.get(i).x;
            BigInteger yi = points.get(i).y;
            
            // Calculate Li(0) = product of (-xj)/(xi-xj) for j != i
            BigInteger termNumerator = yi;
            BigInteger termDenominator = BigInteger.ONE;
            
            System.out.print("L" + (i+1) + "(0) = ");
            StringBuilder calc = new StringBuilder();
            
            for (int j = 0; j < size; j++) {
                if (i == j) continue;
                BigInteger xj = points.get(j).x;
                termNumerator = termNumerator.multiply(xj.negate());
                termDenominator = termDenominator.multiply(xi.subtract(xj));
                calc.append("(-").append(xj).append(")/(").append(xi).append("-").append(xj).append(") × ");
            }
            
            if (calc.length() > 3) {
                calc.setLength(calc.length() - 3); // Remove last " × "
            }
            
            // Add this term to the result using rational arithmetic
            // result = result + termNumerator/termDenominator
            BigInteger newNumerator = resultNumerator.multiply(termDenominator)
                                    .add(termNumerator.multiply(resultDenominator));
            BigInteger newDenominator = resultDenominator.multiply(termDenominator);
            
            // Simplify the fraction by dividing by GCD
            BigInteger gcd = newNumerator.gcd(newDenominator);
            resultNumerator = newNumerator.divide(gcd);
            resultDenominator = newDenominator.divide(gcd);
            
            System.out.println(calc);
            System.out.println("Term " + (i+1) + ": " + yi + " × Li(0)");
        }
        
        // The result should be an integer (the secret)
        if (!resultNumerator.remainder(resultDenominator).equals(BigInteger.ZERO)) {
            System.out.println("⚠️  Warning: Result is not an integer!");
            System.out.println("Final fraction: " + resultNumerator + "/" + resultDenominator);
            return resultNumerator.divide(resultDenominator); // Integer division
        }
        
        BigInteger secret = resultNumerator.divide(resultDenominator);
        System.out.println("\n🎯 Final Secret: " + secret);
        return secret;
    }

    // Main orchestration method
    public SolutionResult findSecret(String jsonInput) {
        System.out.println("🔐 Shamir's Secret Sharing Solver");
        System.out.println("=====================================");
        
        List<Point> points = parseJson(jsonInput);
        
        System.out.println("\n✅ Using points for interpolation:");
        for (Point p : points) {
            System.out.println("   " + p);
        }
        
        // Use Lagrange interpolation
        BigInteger secret = lagrangeInterpolation(points);
        
        System.out.println("\n🎉 RESULT:");
        System.out.println("   Secret: " + secret);
        System.out.println("=====================================");
        
        return new SolutionResult(secret, "Secret computed using Lagrange interpolation method.");
    }

    // Test cases
    public static void main(String[] args) {
        ShamirSecretSolver solver = new ShamirSecretSolver();
        
        // Test Case 1
        String testCase1 = "{\n" +
            "    \"keys\": {\n" +
            "        \"n\": 4,\n" +
            "        \"k\": 3\n" +
            "    },\n" +
            "    \"1\": {\n" +
            "        \"base\": \"10\",\n" +
            "        \"value\": \"4\"\n" +
            "    },\n" +
            "    \"2\": {\n" +
            "        \"base\": \"2\",\n" +
            "        \"value\": \"111\"\n" +
            "    },\n" +
            "    \"3\": {\n" +
            "        \"base\": \"10\",\n" +
            "        \"value\": \"12\"\n" +
            "    },\n" +
            "    \"6\": {\n" +
            "        \"base\": \"4\",\n" +
            "        \"value\": \"213\"\n" +
            "    }\n" +
            "}";
        
        try {
            System.out.println("🧪 Running Test Case 1:");
            SolutionResult result1 = solver.findSecret(testCase1);
            
            // Manual verification for Test Case 1
            System.out.println("\n🔍 Manual Verification for Test Case 1:");
            System.out.println("Base conversions:");
            System.out.println("  x=1, y=4 (base 10)");
            System.out.println("  x=2, y=7 (111 in base 2 = 4+2+1 = 7)");
            System.out.println("  x=3, y=12 (base 10)");
            System.out.println("  x=6, y=39 (213 in base 4 = 2*16+1*4+3 = 39)");
            System.out.println("Using first 3 points: (1,4), (2,7), (3,12)");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test Case 2 (Complex case)
        String testCase2 = "{\n" +
            "\"keys\": {\n" +
            "    \"n\": 10,\n" +
            "    \"k\": 7\n" +
            "  },\n" +
            "  \"1\": {\n" +
            "    \"base\": \"6\",\n" +
            "    \"value\": \"13444211440455345511\"\n" +
            "  },\n" +
            "  \"2\": {\n" +
            "    \"base\": \"15\",\n" +
            "    \"value\": \"aed7015a346d635\"\n" +
            "  },\n" +
            "  \"3\": {\n" +
            "    \"base\": \"15\",\n" +
            "    \"value\": \"6aeeb69631c227c\"\n" +
            "  },\n" +
            "  \"4\": {\n" +
            "    \"base\": \"16\",\n" +
            "    \"value\": \"e1b5e05623d881f\"\n" +
            "  },\n" +
            "  \"5\": {\n" +
            "    \"base\": \"8\",\n" +
            "    \"value\": \"316034514573652620673\"\n" +
            "  },\n" +
            "  \"6\": {\n" +
            "    \"base\": \"3\",\n" +
            "    \"value\": \"2122212201122002221120200210011020220200\"\n" +
            "  },\n" +
            "  \"7\": {\n" +
            "    \"base\": \"3\",\n" +
            "    \"value\": \"20120221122211000100210021102001201112121\"\n" +
            "  },\n" +
            "  \"8\": {\n" +
            "    \"base\": \"6\",\n" +
            "    \"value\": \"20220554335330240002224253\"\n" +
            "  },\n" +
            "  \"9\": {\n" +
            "    \"base\": \"12\",\n" +
            "    \"value\": \"45153788322a1255483\"\n" +
            "  },\n" +
            "  \"10\": {\n" +
            "    \"base\": \"7\",\n" +
            "    \"value\": \"1101613130313526312514143\"\n" +
            "  }\n" +
            "}";
            
        try {
            System.out.println("\n\n🧪 Running Test Case 2 (Complex):");
            SolutionResult result2 = solver.findSecret(testCase2);
            
        } catch (Exception e) {
            System.err.println("❌ Error in Test Case 2: " + e.getMessage());
            e.printStackTrace();
        }
    }
}