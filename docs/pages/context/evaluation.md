## Controlling Order of Evaluation

What we've seen is a general principle for handling potentially unbounded recursive structures: delay building the structure until it is needed.


A classic application of this is in building infinite lists. We already know we can define

, and an extension of Scala's call-by-name parameters. Call-by-name parameters are not evaluated until they are referred to, but this is not sufficient for our purposes. Putting the value into a data structure counts as a reference, which would cause evaluation. We've extended call-by-name parameters to delay evaluation to a point of our choosing, by wrapping the parameter in a zero-argument function. With this technique we have complete control over the order of evaluation. What we've implement, in the `Parser` interpreter, is call-by-name evaluation for delayed parsers. (Note the subtle difference: this is call-by-name evaluation, which is more general than call-by-name parameters.) The distinguishing feature of call-by-name evaluation is that we evaluate delayed values every time they are referred to (which is exactly what our interpreter does). In contrast, call-by-need (also known as lazy evaluation) evaluates the delayed value the first time it is referred to, and stores the result for future use. Call-by-value (also known as eager evaluation) is the default in Scala and has no delayed values.
