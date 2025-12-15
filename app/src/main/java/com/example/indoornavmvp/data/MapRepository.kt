package com.example.indoornavmvp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Repository for loading and saving the navigation map data.
 */
object MapRepository {

    private const val MAP_FILE_NAME = "map.json"
    private val gson = Gson()

    /**
     * Load node DTOs from the map.json file in internal storage.
     * Returns an empty list if the file doesn't exist or is invalid.
     */
    fun loadNodeDtos(context: Context): List<NodeDto> {
        val file = File(context.filesDir, MAP_FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            val type = object : TypeToken<List<NodeDto>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Save a list of LocalNodes to map.json in internal storage.
     * Converts LocalNodes to NodeDtos for JSON serialization.
     */
    fun saveNodes(context: Context, nodes: List<LocalNode>): Boolean {
        return try {
            val dtos = nodes.map { NodeDto.fromLocalNode(it) }
            val json = gson.toJson(dtos)
            val file = File(context.filesDir, MAP_FILE_NAME)
            file.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Build a NavGraph from a list of NodeDtos.
     * 
     * MVP rules:
     * - Nodes are connected sequentially (NODE_0 -> NODE_1 -> NODE_2 -> ...)
     * - Edges are bidirectional with cost = 1.0
     * - A single room "Room 201" is created pointing to the last node
     */
    fun buildNavGraphFromDtos(dtos: List<NodeDto>): NavGraph {
        if (dtos.isEmpty()) {
            return NavGraph(emptyList(), emptyList(), emptyList())
        }

        // Create NavNodes from DTOs
        val navNodes = dtos.map { dto ->
            NavNode(
                id = NodeId("NODE_${dto.id}"),
                label = dto.label
            )
        }

        // Create bidirectional edges connecting nodes sequentially
        val edges = mutableListOf<Edge>()
        for (i in 0 until dtos.size - 1) {
            val fromId = NodeId("NODE_${dtos[i].id}")
            val toId = NodeId("NODE_${dtos[i + 1].id}")
            
            // Forward edge
            edges.add(Edge(from = fromId, to = toId, cost = 1f))
            // Backward edge (for bidirectional navigation)
            edges.add(Edge(from = toId, to = fromId, cost = 1f))
        }

        // MVP: Create a single room pointing to the last node
        val rooms = listOf(
            Room(
                id = "room_201",
                name = "Room 201",
                doorNode = NodeId("NODE_${dtos.last().id}")
            )
        )

        return NavGraph(
            nodes = navNodes,
            edges = edges,
            rooms = rooms
        )
    }

    /**
     * Build a map from NodeId to pose matrix for renderer use.
     */
    fun buildNodePoseMap(dtos: List<NodeDto>): Map<NodeId, FloatArray> {
        return dtos.associate { dto ->
            NodeId("NODE_${dto.id}") to dto.pose.toFloatArray()
        }
    }
}

