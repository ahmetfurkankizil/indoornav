package com.example.indoornavmvp.ar

import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.Session
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Color
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

/**
 * Factory for creating AR nodes (spheres, arrows) for the navigation app.
 */
object ArNodeFactory {

    /**
     * Node marker colors
     */
    object Colors {
        val GREEN = Color(0.0f, 0.8f, 0.2f, 1.0f)      // Admin placed nodes
        val CYAN = Color(0.0f, 0.7f, 1.0f, 1.0f)       // User mode nodes
        val ORANGE = Color(1.0f, 0.4f, 0.0f, 1.0f)     // Navigation arrow
        val RED = Color(1.0f, 0.2f, 0.2f, 1.0f)        // Destination
    }

    /**
     * Create a sphere node for marking navigation waypoints.
     *
     * @param arSceneView The AR scene view to create the node in
     * @param color RGB color values
     * @param radius Sphere radius in meters
     * @return A sphere node
     */
    suspend fun createSphereNode(
        arSceneView: ARSceneView,
        color: Color = Colors.GREEN,
        radius: Float = 0.05f
    ): SphereNode {
        val materialInstance = arSceneView.materialLoader.createColorInstance(
            color
        )

        return SphereNode(
            engine = arSceneView.engine,
            radius = radius,
            materialInstance = materialInstance
        )
    }

    /**
     * Create an anchor node with a sphere marker at the given pose.
     *
     * @param arSceneView The AR scene view
     * @param session The AR session
     * @param pose The pose to anchor at
     * @param color Sphere color
     * @param radius Sphere radius
     * @return An anchor node with a sphere child, or null if anchor creation fails
     */
    suspend fun createSphereAnchorNode(
        arSceneView: ARSceneView,
        session: Session,
        pose: Pose,
        color: Color = Colors.GREEN,
        radius: Float = 0.05f
    ): AnchorNode? {
        return try {
            val anchor = session.createAnchor(pose)
            val sphereNode = createSphereNode(arSceneView, color, radius)

            AnchorNode(
                engine = arSceneView.engine,
                anchor = anchor
            ).apply {
                addChildNode(sphereNode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create an arrow indicator node (cone pointing up + cylinder stem).
     * The arrow points upward (Y-axis) by default.
     *
     * @param arSceneView The AR scene view
     * @param color Arrow color
     * @param scale Overall scale factor
     * @return A node containing the arrow geometry
     */
    suspend fun createArrowNode(
        arSceneView: ARSceneView,
        color: Color = Colors.ORANGE,
        scale: Float = 1.0f
    ): Node {
        val materialInstance = arSceneView.materialLoader.createColorInstance(
            color
        )

        // Create parent node for the arrow
        val arrowNode = Node(arSceneView.engine)

        // Cone (arrow head) - pointing up
        val coneNode = CylinderNode(
            engine = arSceneView.engine,
            radius = 0.08f * scale,
            height = 0.15f * scale,
            materialInstance = materialInstance
        ).apply {
            position = Position(0f, 0.15f * scale, 0f)
            // Make it a cone by scaling top to 0
            this.scale = Scale(1f, 1f, 0.01f)
        }

        // Cylinder (arrow stem)
        val stemNode = CylinderNode(
            engine = arSceneView.engine,
            radius = 0.03f * scale,
            height = 0.2f * scale,
            materialInstance = materialInstance
        ).apply {
            position = Position(0f, 0f, 0f)
        }

        arrowNode.addChildNode(stemNode)
        arrowNode.addChildNode(coneNode)

        return arrowNode
    }

    /**
     * Create an anchor node with an arrow at the given pose.
     *
     * @param arSceneView The AR scene view
     * @param session The AR session
     * @param pose The pose to anchor at
     * @param color Arrow color
     * @param scale Arrow scale
     * @return An anchor node with an arrow child, or null if anchor creation fails
     */
    suspend fun createArrowAnchorNode(
        arSceneView: ARSceneView,
        session: Session,
        pose: Pose,
        color: Color = Colors.ORANGE,
        scale: Float = 1.0f
    ): AnchorNode? {
        return try {
            val anchor = session.createAnchor(pose)
            val arrowNode = createArrowNode(arSceneView, color, scale)

            AnchorNode(
                engine = arSceneView.engine,
                anchor = anchor
            ).apply {
                addChildNode(arrowNode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create an anchor node with a cube marker (alternative to sphere).
     *
     * @param arSceneView The AR scene view
     * @param session The AR session
     * @param pose The pose to anchor at
     * @param color Cube color
     * @param size Cube size in meters
     * @return An anchor node with a cube child, or null if anchor creation fails
     */
    suspend fun createCubeAnchorNode(
        arSceneView: ARSceneView,
        session: Session,
        pose: Pose,
        color: Color = Colors.GREEN,
        size: Float = 0.08f
    ): AnchorNode? {
        return try {
            val anchor = session.createAnchor(pose)
            val materialInstance = arSceneView.materialLoader.createColorInstance(
                color
            )

            val cubeNode = CubeNode(
                engine = arSceneView.engine,
                size = Position(size, size, size),
                materialInstance = materialInstance
            )

            AnchorNode(
                engine = arSceneView.engine,
                anchor = anchor
            ).apply {
                addChildNode(cubeNode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create an anchor from a saved pose matrix (16 floats).
     *
     * @param session The AR session
     * @param poseMatrix The 4x4 transformation matrix as 16 floats
     * @return An anchor at the pose, or null if creation fails
     */
    fun createAnchorFromMatrix(
        session: Session,
        poseMatrix: FloatArray
    ): Anchor? {
        return try {
            // Extract translation from indices 12, 13, 14
            val pose = Pose.makeTranslation(poseMatrix[12], poseMatrix[13], poseMatrix[14])
            session.createAnchor(pose)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert a Pose to a 16-float matrix array.
     */
    fun poseToMatrix(pose: Pose): FloatArray {
        val matrix = FloatArray(16)
        pose.toMatrix(matrix, 0)
        return matrix
    }
}


