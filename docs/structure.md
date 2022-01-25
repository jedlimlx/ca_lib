# Structure

## Pattern Representation

In ca_lib, patterns are represented by the abstract [Grid](api-reference/ca_lib/simulation/-grid/index.html) class.

There are multiple different implementations of this class, better suited for different circumstances:

- [SparseGrid](api-reference/ca_lib/simulation/-sparse-grid/index.html): This implementation makes use of a sparse matrix to represent the cells. 
As such, it is better suited to running larger patterns in which there is a still amount of empty space between active regions.
- [DenseGrid](api-reference/ca_lib/simulation/-dense-grid/index.html): This implementation makes use of a 2D array to represent the cells.
For small, dense patterns, this implementation will use less memory and simulate more quickly as reading and writing to an array is more efficient than
reading and writing to a sparse matrix (represented by a map). However, since empty regions are stored as well, this method will result in a higher memory usage for larger patterns.


## Cellular Automaton Rules

In ca_lib, individual rules are represented by the abstract [Rule](api-reference/ca_lib/rules/-rule/index.html) class.
A family of rules such as INT rules or HROT rules are represented by the abstract [RuleFamily](api-reference/ca_lib/rules/-rule-family/index.html) class.

The currently supported rulefamiles are:

- Higher-range Outer Totalistic (HROT)
- Isotropic non-totalistic (INT)

To implement your own rule families and rules, see [custom-rules.md]