Let's write code to calculate the factorial of a number in several languages.  The factorial of a non-negative integer n, denoted by n!, is the product of all positive integers less than or equal to n.  For example, 5! = 5 * 4 * 3 * 2 * 1 = 120.

```python
# Python
def factorial(n):
  """Calculates the factorial of a non-negative integer."""
  if n < 0:
    raise ValueError("Factorial is not defined for negative numbers.")
  elif n == 0:
    return 1
  else:
    result = 1
    for i in range(1, n + 1):
      result *= i
    return result

print(factorial(5))  # Output: 120
```

Python uses a simple iterative approach.  The `if-elif-else` structure handles edge cases (negative input and base case of 0!).  The loop iteratively multiplies numbers from 1 to n.

```javascript
// JavaScript
function factorial(n) {
  //Error Handling for negative input
  if (n < 0) {
    throw new Error("Factorial is not defined for negative numbers.");
  } else if (n === 0) {
    return 1;
  } else {
    let result = 1;
    for (let i = 1; i <= n; i++) {
      result *= i;
    }
    return result;
  }
}

console.log(factorial(5)); // Output: 120
```

JavaScript's implementation mirrors Python's, demonstrating the similarity in fundamental programming concepts across languages.  Error handling is included for negative input.


```java
// Java
public class Factorial {
  public static int factorial(int n) {
    if (n < 0) {
      throw new IllegalArgumentException("Factorial is not defined for negative numbers.");
    } else if (n == 0) {
      return 1;
    } else {
      int result = 1;
      for (int i = 1; i <= n; i++) {
        result *= i;
      }
      return result;
    }
  }

  public static void main(String[] args) {
    System.out.println(factorial(5)); // Output: 120
  }
}
```

Java requires a class structure.  The `main` method is the entry point.  The `IllegalArgumentException` is thrown for invalid input, similar to the other examples.


```c++
// C++
#include <iostream>
#include <stdexcept>

int factorial(int n) {
  if (n < 0) {
    throw std::invalid_argument("Factorial is not defined for negative numbers.");
  } else if (n == 0) {
    return 1;
  } else {
    int result = 1;
    for (int i = 1; i <= n; i++) {
      result *= i;
    }
    return result;
  }
}

int main() {
  std::cout << factorial(5) << std::endl; // Output: 120
  return 0;
}
```

C++ includes the `<iostream>` for input/output and `<stdexcept>` for exception handling.  The structure is similar to Java, but with a more concise syntax.


```c#
// C#
using System;

public class Factorial
{
    public static int CalculateFactorial(int n)
    {
        if (n < 0)
        {
            throw new ArgumentException("Factorial is not defined for negative numbers.");
        }
        else if (n == 0)
        {
            return 1;
        }
        else
        {
            int result = 1;
            for (int i = 1; i <= n; i++)
            {
                result *= i;
            }
            return result;
        }
    }

    public static void Main(string[] args)
    {
        Console.WriteLine(CalculateFactorial(5)); // Output: 120
    }
}
```

C# uses a class structure similar to Java.  The `Main` method is the entry point.  Exception handling is implemented using `ArgumentException`.


These examples highlight the core similarities and minor syntactic differences in how the same problem is solved across different programming languages.  The fundamental logic remains consistent while the specific keywords and syntax vary.