# Designing Using Type Classes

We're now going to design a good portion of the parser interface by using the common type classes as inspiration. One view of type classes is that we implement them because doing gives us a range of methods "for free". A different view is that the type class abstractions exist because they capture common patterns, so when we implement our own systems we can look to the type classes for inspiration. This is the approach we're taking here, though we'll also benefit from the additional methods that type classes instances bring.

Your tasks are to:

- consider each type class interface and see if it makes sense in the context of parsers;
- implement the methods and type class instance if appropriate; and
- implement tests for any methods you've created.

To get started I'll show you how to do the first one, `Functor`.
