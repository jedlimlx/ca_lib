# Getting Started

## Installation

...

## Examples

**Editing and reading cells**
```kotlin
val grid = SparseGrid()
grid[0, 0] = 1
grid[0, 1] = 1
grid[0, 2] = 1
println(grid) // 3o!
println(grid[1, 0]) // 0
println(grid[0, 1]) // 1
```

**Running simulations**
```kotlin
val grid = SparseGrid("b2o\$2ob\$bob!", HROT("B3/S23"))
println(grid.step(100))  // The 100th generation of the r-pentamino
```

**Pattern Identification**
```kotlin
val grid = SparseGrid("2ob\$b2o\$o2b!", HROT("B3/S23"))
val pattern = grid.identify() as Spaceship

println(pattern)  // c/4d Spaceship
println(pattern.direction)  // Diagonal
println(pattern.heat.joinToString(" ")) // 4 4 4 4
```
