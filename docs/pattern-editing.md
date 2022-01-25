# Pattern Editing

In addition to running patterns, ca_lib has extensive support for editing
patterns. The syntax for pattern editing in ca_lib is meant to emulate that of lifelib.
The following operations on patterns are supported:

- **Shifting, rotating and flipping patterns**
    - Shifting is done via `#!kotlin grid.shift(dx, dy)` or the more concise `#!kotlin grid(dx, dy)`.
    - Rotation is done via `#!kotlin grid.rotate(Rotation.CLOCKWISE)` or `#!kotlin grid(Rotation.CLOCKWISE)`.
    - Patterns can be flipped via `#!kotlin grid.flip(Flip.HORIZONTAL)`.
    - To apply this on a rectangular sub-region, Kotlin's range syntax which has been overloaded
      can be applied as such `#!kotlin grid.shift(Coordinate(startX, startY) .. Coordinate(endX, endY), dx, dy)`.

- **Getting and setting individual cells**
    - Individual cells can be accessed as such `#!kotlin grid[x, y]` or
      with the [Coordinate](api-reference/ca_lib/simulation/-coordinate/index.html) class as such `#!kotlin grid[Coordinate(x, y)]`.

- **Slicing rectangular sub-regions**
    - There are 2 ways to slice a grid into rectangular sub-regions.
    - Either `#!kotlin grid[Coordinate(startX, startY) .. Coordinate(endX, endY)]` or `#!kotlin grid[startX .. endX, startY .. endY]`.
    - For the 2nd method, you can even do `#!kotlin grid[startX .. endX step 2, startY .. endY step 2]`.

- **Bitwise Operations**
    - Bitwise operations on grids are supported via operater overloading.
    - Union / addition can be done as such `#!kotlin grid1 union grid2` or `#!kotlin grid1 + grid2`
    - Intersection / add can be done as such `#!kotlin grid1 and grid2` or `#!kotlin grid1 intersect grid2`
    - The grid can also be inverted with `#!kotlin grid.invert(Coordinate(startX, startY) .. Coordinate(endX, endY))`

- **Direct insertion of RLEs / Apgcodes / other grids**
    - One may write `#!kotlin grid[x, y] = pattern` or `#!kotlin grid[Coordinate(x, y)] = pattern` where x and y are the coordinates to place the pattern's top-left corner
    - ca_lib makes use of regex to tell the difference between RLEs and apgcodes.
    - To prevent this overhead, one may use [addRLE](api-reference/ca_lib/simulation/-grid/add-r-l-e.html) or
      [addApgcode](api-reference/ca_lib/simulation/-grid/add-apgcode.html) instead.

- **Iteration across cells**
    - Since the [Grid](api-reference/ca_lib/simulation/-grid/index.html) class inherits from Iterable<Pair<Coordinate, Int>>,
      methods that apply to Iterables also apply to [Grid](api-reference/ca_lib/simulation/-grid/index.html).
    - For example, one may write some of the following:
        - `#!kotlin grid.forEach { (coordinate, state) -> grid[coordinate] = 1 }` which sets the state of all cells with state > 1 to 1
        - `#!kotlin grid.map { it.first }` which returns a list of all the coordinates of live cells
        - `#!kotlin grid.fold(0) { acc, value -> acc + value.second }` which returns the sum of all states in the grid
        - `#!kotlin grid.count { it.second == 2 }` which returns the number of cells with state == 2
        - etc.

- **Pattern Matching**
    - Pattern matching in ca_lib is currently inefficient. Work is being done to optimise the (currently naive) algorithm.
    - A template pattern can be matched like this `#!kotlin grid.findAll(template)` and all instance of said 
      pattern can be replaced with `#!kotlin grid.replaceAll(template, newPattern)`

For more information, read the documentation for the [Grid](api-reference/ca_lib/simulation/-grid/index.html) class.
