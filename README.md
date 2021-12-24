# ca_lib

ca_lib is a simple and feature-rich cellular automaton simulation library written in pure Kotlin.

## Goals
### 1. Simple and Intuitive Interface

* ca_lib's APIs should be simple to understand and easy to grasp

### 2. Less Boilerplate

* Just like Kotlin, ca_lib aims to reduce the amount of boilerplate code
* Commonly needed functions should all be implemented, negating the need for the user to implement them themselves

### 3. Multiplatform

* ca_lib should ideally be able to work on Kotlin's Java, Javascript and Native backends in order to support as many platforms as possible


# Usage

## Download

```
<Insert gradle stuff>
```


```
<Insert npm stuff>
```

## Examples

### Simulation

```kotlin
val grid = SparseGrid("boo\$oob\$bob!", HROT("B3/S23"))
println(grid.step(1000))  // Outputs the RLE
```

### Pattern Manipulation
**Translation**
```kotlin

```

**Rotation**
```kotlin

```

**Flipping**
```kotlin

```

### Pattern Identification
```kotlin
val grid = SparseGrid("ooo\$obb\$bob!", HROT("B3/S23"))

// Identification
val glider = grid.identify() as Spaceship

// Look at its various properties
println(glider.ruleRange)  // The glider's rule range
println(glider.speed)  // c/4d
println(glider.populationList)  // A list of the population of the glider in each of its phases
```

### Ruletable Generation
```kotlin
println(ruletable {
    name = "StarWars"
    table(4, symmetry = PermuteSymmetry()) {
        variable("any") { 0..3 }
        variable("dead") { listOf(0, 2, 3) }

        comment("Birth on 2")
        transition { "0 1 1 dead dead dead dead dead dead 1" }

        comment("Survival on 3, 4, 5")
        transitions {
            listOf(
                "1 1 1 1 dead dead dead dead dead 1",
                "1 1 1 1 1 dead dead dead dead 1",
                "1 1 1 1 1 1 dead dead dead 1"
            )
        }

        comment("Everything else dies")
        transitions {
            listOf(
                "1 any any any any any any any any 2",
                "2 any any any any any any any any 3",
                "3 any any any any any any any any 0"
            )
        }
    }
})
```

# Features
- Simulation
- Pattern Manipulation
- Pattern Identification
- Rule Range Enumeration
- Generating and Enumerating Soups
- Ruletable Generation
- Support for many rulespaces
  - Higher Range Outer-Totalistic
    - 2-state
    - Generations
  - Isotropic Non-Totalistic
    - 2-state
    - Generations

# Planned for the future
- Support for more rulespaces
- Constellation Enumeration
- Synthesis Enumeration
- Object Separation
- Further Optimisations