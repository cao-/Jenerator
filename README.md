# Jenerators

This is a simple Java implementation of [generators](https://en.wikipedia.org/wiki/Generator_(computer_programming)), that resemble those found in the Python language.  They are just generators (that is, a convenient way to create iterators), not full coroutines.

## Examples:
Here are some examples that show how Python generators can be translated into Java Jenerators.
* Generate all integer values.
  - Python
    ```python
    def integers():
        i = 0
        while True:
            yield i
            i += 1
    
    it = integers()
    print(next(it))
    print(next(it))
    print(next(it))
    ```
  - Java
    ```java
    var integers = new Jenerator<Integer>(yield -> {
        for (int i = 0; ; ++i) yield.accept(i);
    });
    
    var it = integers.iterator();
    System.out.println(it.next());
    System.out.println(it.next());
    System.out.println(it.next());
    ```
  - Output
    ```
    0
    1
    2
    ```
* Generate the truth table of size n.
  - Python
    ```python
    def truth_values(n):
        a = [True] * n
        def go(i):
            if i == 0:
                yield a
            else:
                a[n - i] = True
                yield from go(i - 1)
                a[n - i] = False
                yield from go(i - 1)
        return go(n)
    
    for a in truth_values(3)
        print(a)
    ```
  - Java
    ```java
    Function<Integer, Jenerator<boolean[]>> truthValues = n -> new Jenerator<>(yield -> {
        boolean[] a = new boolean[n];
        var go = new Consumer<Integer>() {
            public void accept(Integer i) {
                if (i == 0) {
                    yield.accept(a);
                } else {
                    a[n - i] = true;
                    this.accept(i - 1);
                    a[n - i] = false;
                    this.accept(i - 1);
                }
            }
        };
        go.accept(n);
    });

    for (var a : truthValues.apply(3)) System.out.println(Arrays.toString(a));
    ```
  - Output
    ```
    [true, true, true]
    [true, true, false]
    [true, false, true]
    [true, false, false]
    [false, true, true]
    [false, true, false]
    [false, false, true]
    [false, false, false]
    ```
* Generate all combinations of k elements from list x.
  - Python
    ```python
    def gcomb(x, k):
        if k > len(x):
            return
        if k == 0:
            yield []
        else:
            first, rest = x[0], x[1:]
            for c in gcomb(rest, k - 1):
                c.insert(0, first)
                yield c
            for c in gcomb(rest, k):
                yield c
    
    seq = list(range(1, 5))
    for k in range(len(seq) + 2):
        print(f"{k}-combs of {seq}:")
        for c in gcomb(seq, k):
            print("   ", c)
    ```
  - Java
    ```java
    <T> Jenerator<List<T>> gComb(List<T> x, int k) {
        return new Jenerator<>(yield -> {
            if (k > x.size()) return;
            if (k == 0) yield.accept(new LinkedList<>());
            else {
                var first = x.getFirst();
                var rest = x.stream().skip(1).toList();
                for (var c : gComb(rest, k - 1)) {
                    c.addFirst(first);
                    yield.accept(c);
                }
                for (var c : gComb(rest, k))
                    yield.accept(c);
            }
        });
    }
    
    var seq = IntStream.range(1, 5).boxed().toList();
    IntStream.range(0, seq.size() + 2).forEach(k -> {
        System.out.printf("%d-combs of %s%n", k, seq);
        for (var c : gComb(seq, k))
            System.out.println("   " + c);
    });
    ```
    - Output
    ```
    0-combs of [1, 2, 3, 4]
    []
    1-combs of [1, 2, 3, 4]
    [1]
    [2]
    [3]
    [4]
    2-combs of [1, 2, 3, 4]
    [1, 2]
    [1, 3]
    [1, 4]
    [2, 3]
    [2, 4]
    [3, 4]
    3-combs of [1, 2, 3, 4]
    [1, 2, 3]
    [1, 2, 4]
    [1, 3, 4]
    [2, 3, 4]
    4-combs of [1, 2, 3, 4]
    [1, 2, 3, 4]
    5-combs of [1, 2, 3, 4]
    ```
    