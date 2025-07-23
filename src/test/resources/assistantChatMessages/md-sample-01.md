```typescript
/**
 * This function takes an array of numbers and returns the sum of all even numbers in the array.
 * If the input array is empty or null/undefined, it returns 0.
 *
 * @param numbers An array of numbers.
 * @returns The sum of all even numbers in the array.
 */
function sumOfEvens(numbers: number[]): number {
  if (!numbers || numbers.length === 0) {
    return 0;
  }

  let sum: number = 0;
  for (const number of numbers) {
    if (number % 2 === 0) {
      sum += number;
    }
  }
  return sum;
}


/**
 * This function takes a string and returns a new string with all vowels removed.
 *
 * @param str The input string.
 * @returns A new string with all vowels removed.  Returns an empty string if input is null or undefined.
 */
function removeVowels(str: string | null | undefined): string {
  if (str === null || str === undefined) {
    return "";
  }
  const vowels: string = "aeiouAEIOU";
  let result: string = "";
  for (let i: number = 0; i < str.length; i++) {
    if (!vowels.includes(str[i])) {
      result += str[i];
    }
  }
  return result;
}


// Example usage
const numbers: number[] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const sum: number = sumOfEvens(numbers);
console.log(`Sum of even numbers: ${sum}`); // Output: Sum of even numbers: 30

const myString: string = "Hello, World!";
const stringWithoutVowels: string = removeVowels(myString);
console.log(`String without vowels: ${stringWithoutVowels}`); // Output: String without vowels: Hll, Wrld!

const nullString: string | null = null;
const resultNullString = removeVowels(nullString);
console.log(`Result of null string: ${resultNullString}`); // Output: Result of null string: 

const undefinedString: string | undefined = undefined;
const resultUndefinedString = removeVowels(undefinedString);
console.log(`Result of undefined string: ${resultUndefinedString}`); // Output: Result of undefined string: 
```