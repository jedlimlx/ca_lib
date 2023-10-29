# ca_lib [![test](https://github.com/jedlimlx/ca_lib/actions/workflows/gradle.yml/badge.svg)](https://github.com/jedlimlx/ca_lib/actions/workflows/gradle.yml)

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



# What currently works
- **Pattern manipulation** - Rotation, shifting, flipping, etc.
- **Pattern identification** - Identification of spaceships and oscillator
- **Rule range enumeration**
- **Generating and enumerating soups** of symmetries C1, D2- and D4+
- **Ruletable generation** - Support for B0 and unbounded rules
- **Support for many rulespaces**
  - Higher Range Outer-Totalistic
    - 2-state
    - Generations
    - Extended Generations
  - Isotropic Non-Totalistic
    - 2-state
    - Generations

# What's planned for the future
- **Support for more rulespaces** such as BSFKL, naive rules, etc.
- **Constellation Enumeration** to generate all combinations of dots / blinkers / block etc.
- **Synthesis Enumeration** to generate all possible n-glider collisions
- **Object Separation** like what apgsearch does
- **Further Optimisations** such as implementing HashLife
