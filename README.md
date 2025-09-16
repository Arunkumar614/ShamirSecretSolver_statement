this statement implements a Shamir’s Secret Sharing polynomial reconstruction in Java.
It reconstructs the original secret (constant term of the polynomial) using Lagrange Interpolation from encoded shares provided in JSON format.

✨ Features

✅ No External Dependencies – uses a custom JSON parser (no Gson or Jackson needed)

✅ BigInteger Precision – exact arithmetic for large numbers

✅ Lagrange Interpolation – direct computation of P(0) (the secret)

✅ Multi-base Support – handles bases 2–36 (alphanumeric digits)

✅ Detailed Logging – step-by-step conversion, interpolation, and results

✅ Multiple Test Cases – runs provided test inputs

📊 Example Output (Test Case 1)
🧪 Running Test Case 1:
🔐 Shamir's Secret Sharing Solver
=====================================
📊 Problem Parameters:
   Total shares (n): 4
   Required shares (k): 3
   Polynomial degree: 2

🔢 Base Conversions:
   x=1: base10("4") = 4
   x=2: base2("111") = 7
   x=3: base10("12") = 12
   x=6: base4("213") = 39

✅ Using points for interpolation:
   (1, 4)
   (2, 7)
   (3, 12)

🔍 Using Lagrange Interpolation:
[Calculation steps...]

🎯 Final Secret: 3

🎉 RESULT:
   Secret: 3
=====================================

🧮 Verification (Test Case 1)

Points: (1,4), (2,7), (3,12)

Polynomial: P(x) = ax² + bx + c

System:

a + b + c = 4

4a + 2b + c = 7

9a + 3b + c = 12

Solving → a = 1/2, b = 5/2, c = 3

✅ Secret = P(0) = 3

📘 Input JSON Format
{
  "n": 4,
  "k": 3,
  "shares": {
    "1": { "base": 10, "value": "4" },
    "2": { "base": 2,  "value": "111" },
    "3": { "base": 10, "value": "12" },
    "6": { "base": 4,  "value": "213" }
  }
}


n: Total number of shares

k: Required number of shares

shares: Mapping of x → {base, value}
