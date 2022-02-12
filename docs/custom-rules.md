# Custom Rules

## Individual Rules

## Rule Families

## Rule Tables
To construct rule tables, ca_lib makes use of a very useful feature of Kotlin known as type-safe builders.
This allows rule tables to be constructed programmatically using a DSL-like syntax.

For instance,
```kotlin
val ruletable = ruletable {
    name = "Frogs"
    table(numStates = 3, neighbourhood = moore(1)) {
        variable("any") { 0 .. 2 }
        variable("dead") { listOf(0, 2) }
        
        comment("Birth")
        outerTotalistic {
            input = "dead"
            output = "1"
            transitions { 3 .. 4 }
        }

        comment("Survival")
        outerTotalistic {
            input = "1"
            output = "1"
            transitions { 1 .. 2 }
        }

        comment("Everything else dies")
        transition { "1 ${"any ".repeat(8)}2" }
        transition { "2 ${"any ".repeat(8)}0" }
    }
    
    colours(numStates = 3) { 
        listOf(
            Colour(0, 0, 0), 
            Colour(255, 255, 0), 
            Colour(255, 0, 0)
        )[it] 
    }
}.toString()
```

These ruletables are made to be used by apgsearch. To use them in Golly, the neighbourhood section in the outputted ruletable
should be replaced by the relevant named Golly neighbourhood.
