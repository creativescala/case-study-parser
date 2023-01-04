## Parsing JSON

Now we know how to parse recursive expressions we can practice on a larger example: JSON. If you head over to [json.org](https://www.json.org/) you'll find a "railway diagram" defining the syntax of JSON. Your mission is to implement a JSON parser using this syntax as a reference. There are a few fiddly parts of the syntax that you can choose to skip: backslash escaped characters in strings, and the scientific notation for numbers (numbers such as `1.2E10`).

In solving this problem you might find that our current `Parser` API is still a bit lacking. Don't be afraid to extend it if needed.
