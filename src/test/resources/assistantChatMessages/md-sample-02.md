```javascript
// Function to calculate the factorial of a number
function factorial(n) {
  if (n === 0) {
    return 1;
  } else if (n < 0) {
    return "Factorial is not defined for negative numbers.";
  } else {
    let result = 1;
    for (let i = 1; i <= n; i++) {
      result *= i;
    }
    return result;
  }
}

// Example usage:
console.log(factorial(5)); // Output: 120
console.log(factorial(0)); // Output: 1
console.log(factorial(-3)); // Output: Factorial is not defined for negative numbers.


// Function to check if a number is prime
function isPrime(num) {
  if (num <= 1) return false;
  if (num <= 3) return true;

  // This optimization skips checking divisibility by 2 and 3 after initial checks
  if (num % 2 === 0 || num % 3 === 0) return false;

  // Iterate only through 6k ± 1 numbers up to the square root of num
  for (let i = 5; i * i <= num; i += 6) {
    if (num % i === 0 || num % (i + 2) === 0) return false;
  }

  return true;
}

// Example usage:
console.log(isPrime(7)); // Output: true
console.log(isPrime(10)); // Output: false


// Function to reverse a string
function reverseString(str) {
  return str.split("").reverse().join("");
}

// Example usage:
console.log(reverseString("hello")); // Output: olleh


// Function to find the largest element in an array
function findLargest(arr) {
  if (arr.length === 0) {
    return undefined; // Handle empty array case
  }

  let largest = arr[0];
  for (let i = 1; i < arr.length; i++) {
    if (arr[i] > largest) {
      largest = arr[i];
    }
  }
  return largest;
}

// Example usage:
console.log(findLargest([1, 5, 2, 8, 3])); // Output: 8
console.log(findLargest([])); // Output: undefined



// Function to check if a string is a palindrome
function isPalindrome(str) {
  const reversedStr = str.split("").reverse().join("");
  return str === reversedStr;
}


// Example Usage
console.log(isPalindrome("racecar")); // Output: true
console.log(isPalindrome("hello")); // Output: false


```