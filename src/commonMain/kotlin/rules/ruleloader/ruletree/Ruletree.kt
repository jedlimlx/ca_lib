package rules.ruleloader.ruletree

//
//import rules.ruleloader.RuleDirective
//import simulation.Coordinate
//
//
///**
// * Implements Golly ruletrees with additional features such as arbitary neighbourhoods.
// * See http://golly.sourceforge.net/Help/formats.html for more information.
// */
//class Ruletree
///**
// * Constructs a ruletree with the provided content
// * @param content Content to use to construct the ruletree
// */
//    (content: String?) : RuleDirective(content!!), Exportable, kotlin.Cloneable {
//    /**
//     * Content of the ruletree
//     */
//    private var content: String? = null
//
//    /**
//     * List of nodes used by the ruletree
//     */
//    private var nodeList: Array<Node<*>>
//
//    /**
//     * Neighbourhood of the ruletree
//     */
//    var neighbourhood: Array<Coordinate>
//        private set
//
//    /**
//     * Parses the content of the ruletree
//     * @param content The content of the ruletree
//     */
//    fun parseContent(content: String) {
//        this.content = content
//
//        var nodeNumber = 0 // Keep track of the current node number
//        for (line in content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
//            if (line.startsWith("num_states")) {
//                numStates = line.replace("num_states=", "").toInt()
//            } else if (line.startsWith("num_neighbo")) {  // Account for british and american spelling
//                neighbourhood = neighbourhood
//            } else if (line.startsWith("num_nodes")) {
//                nodeList = arrayOfNulls<Node<*>>(line.replace("num_nodes=", "").toInt())
//            } else if (line.startsWith("tiling")) {
//                tiling = getTiling(line.replace("tiling=", ""))
//            } else if (line.matches("(\\d+\\s?)+".toRegex())) {
//                val tokens = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                val children = IntArray(tokens.size - 1)
//                for (i in 1 until tokens.size) {
//                    children[i - 1] = tokens[i].toInt()
//                }
//
//                nodeList[nodeNumber] = Node<Any?>(tokens[0].toInt(), nodeNumber, children)
//                nodeNumber++
//            }
//        }
//    }
//
//    fun transitionFunc(neighbours: IntArray, cellState: Int): Int {
//        var currentNodeNumber = nodeList.size - 1 // Begin at root node
//        for (neighbour in neighbours) {  // Going down the tree
//            currentNodeNumber = nodeList[currentNodeNumber].getChildren().get(neighbour)
//        }
//
//        return nodeList[currentNodeNumber].getChildren().get(cellState) // Finally get the value
//    }
//
//    override fun clone(): Any {
//        return Ruletree(content)
//    }
//
//    override fun export(): String {
//        return content!!
//    }
//}