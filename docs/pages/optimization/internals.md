## Internal Optimizations

In this section we'll look at internal optimizations that are invisible to the end user.


### Memoization

Our current implementation of `delay` re-evaluates the delayed `Parser` every time it is encountered in the interpreter. This is wasteful as the `Parser` will not change between evaluations. We can store the value after we first evaluate it, a technique known as [memoization](https://en.wikipedia.org/wiki/Memoization). 

Scala's `lazy val` makes it straightforward to implement memoization. 


### Rewriting
