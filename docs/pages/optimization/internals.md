## Internal Optimizations

In this section we'll look at internal optimizations that are invisible to the end user.


### Memoization

Our current implementation of `delay` re-evaluates the delayed `Parser` every time it is encountered in the interpreter. This is wasteful as the `Parser` will not change between evaluations. We can store the value after we first evaluate it, a technique known as [memoization](https://en.wikipedia.org/wiki/Memoization). 

Scala's `lazy val` makes memoization straightforward. A `lazy val` is like a `val` except the right-hand side expression, which produces the value, is not evaluated until the `lazy val` is first used. The value is then saved and used for subsequent uses. The following example shows these semantics. Note the `println` is not evalutes until the first use, but future uses do not re-evaluate the `println`.

```scama mdoc
lazy val a = { 
  println("Evaluating")
  1
}
a
a
```


### Rewriting
