# Pattern Analysis

## Identification

Other than editing patterns, ca_lib also provides tools to analyse and view patterns easily.

To identify patterns, one may use the `grid.identify(maxGenerations)` function where maxGenerations 
is the maximum number of generations to check for the pattern. null is returned if no pattern was found.

This function returns a [Pattern](api-reference/ca_lib/patterns/-pattern/index.html) object. Currently, only idenfication of
Oscillators and Spaceships is supported.

Various properties of these patterns can be queried. For example,
```kotlin
val grid = SparseGrid("12bobo2$11bo3bo2$14bo2$5bobo5bo$3bobo$4bo11bobo$6bo$15bo2$16bo$10bobo2$9bo3bo$14bobo3bo$10bo" +
        "$17bobo$11bo$4bobo$10bobo$7bo6\$o5bo2\$bo3bobobo14$8bobo2$7bo3bo2" +
        "$10bo2$9bo2$8bobo14$7bobo2$10bo!", HROT("R2,C0,S1,B2,N@20240A"))
val oscillator = grid.identify() as Oscillator

println(oscillator.period)  // 576
println(oscillator.volatility)  // The volatility of the oscillator
println(oscillator.strictVolatility)  // The strict volatility of the oscillator
println(oscillator.cellPeriods)  // The period of every active cell in the oscillator
```

| Property          | Description                                                                            | Pattern Type           |
|-------------------|----------------------------------------------------------------------------------------|------------------------|
| period            | The period of the pattern                                                              | Oscillator / Spaceship |
| dx                | The displacement of the pattern in the x-direction                                     | Spaceship              |
| dy                | The displacement of the pattern in the y-direction                                     | Spaceship              |
| speed             | The speed of the pattern                                                               | Spaceship              |
| direction         | The direction in which the pattern moves (orthonal, diagonal, etc.)                    | Spaceship              |
| ruleRange         | The range of rules the spaceship works in                                              | Oscillator / Spaceship |
| phases            | The phases of the patttern                                                             | Oscillator / Spaceship |
| heat              | The number of cells that change state in each generation                               | Oscillator / Spaceship |
| populationList    | A list of the population of the pattern at each phase                                  | Oscillator / Spaceship |
| cellPeriods       | A map containing the periods of each active cell in the oscillator                     | Oscillator             |
| rotorCells        | The number of rotor cells in the oscillator                                            | Oscillator             |
| statorCells       | The number of stator cells in the oscillator                                           | Oscillator             |
| activeCells       | The number of active cells in the oscillator                                           | Oscillator             |
| temperature       | The temperature of the oscillator                                                      | Oscillator             |
| volatility        | The fraction of cells in the oscillator that are oscillating                           | Oscillator             |
| strictVolatility  | The fraction of cells in the oscillator that oscillate at the oscillator's full period | Oscillator             |
| nonTrivial        | Whether the oscillator is trivial                                                      | Oscillator             |

!!! note

    The Pattern class is immutable. Attempts to modify its properties after creation will likely result in undefined behaviour.

See [Oscillator](api-reference/ca_lib/patterns/-oscillator/index.html) 
and [Spaceship](api-reference/ca_lib/patterns/-spaceship/index.html) for more information on supported properties.

## Viewing

ca_lib also supports viewing of patterns through export of patterns to images (in particular SVGs).

```kotlin
val grid = SparseGrid(generateC1(32, 32).toRLE(), rule = HROTGenerations("/04/4V"))
println(grid.animatedSvg(1200, transparent = false, step = 4, duration = 25000))
```
![](generate_svg.svg)
