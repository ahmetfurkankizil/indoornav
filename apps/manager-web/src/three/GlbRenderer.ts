import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

export interface RenderBounds {
  minX: number; maxX: number; minZ: number; maxZ: number; floorY: number
}

export interface GlbRenderResult {
  imageDataUrl: string
  bounds: RenderBounds
}

export async function renderGlbTopDown(glbUrl: string, token?: string): Promise<GlbRenderResult> {
  return new Promise((resolve, reject) => {
    const scene = new THREE.Scene()
    scene.background = new THREE.Color(0xf0f0f0)

    const ambientLight = new THREE.AmbientLight(0xffffff, 2.5)
    scene.add(ambientLight)

    const dirLight = new THREE.DirectionalLight(0xffffff, 1.5)
    dirLight.position.set(10, 50, 10)
    scene.add(dirLight)

    const loader = new GLTFLoader()
    if (token) {
      loader.setRequestHeader({ Authorization: `Bearer ${token}` })
    }

    loader.load(
      glbUrl,
      (gltf) => {
        scene.add(gltf.scene)

        const box = new THREE.Box3().setFromObject(gltf.scene)
        const minX = box.min.x
        const maxX = box.max.x
        const minZ = box.min.z
        const maxZ = box.max.z
        const floorY = box.min.y

        const cx = (minX + maxX) / 2
        const cz = (minZ + maxZ) / 2
        const margin = Math.max(maxX - minX, maxZ - minZ) * 0.04 + 0.1

        // Orthographic camera looking straight down (-Y).
        // up = (0,0,-1) so: viewport top = world minZ, viewport bottom = world maxZ.
        // This gives: canvasNormX = (worldX - minX) / (maxX - minX)
        //             canvasNormY = (worldZ - minZ) / (maxZ - minZ)
        const camera = new THREE.OrthographicCamera(
          minX - margin,    // left
          maxX + margin,    // right
          -(minZ - margin), // top  (camera +Y = world -Z)
          -(maxZ + margin), // bottom
          -200,
          200,
        )
        camera.position.set(cx, 50, cz)
        camera.lookAt(cx, 0, cz)
        camera.up.set(0, 0, -1)

        const SIZE = 1024
        const renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true })
        renderer.setSize(SIZE, SIZE)
        renderer.setPixelRatio(1)
        renderer.render(scene, camera)

        const imageDataUrl = renderer.domElement.toDataURL('image/png')
        renderer.dispose()
        renderer.forceContextLoss()

        resolve({ imageDataUrl, bounds: { minX, maxX, minZ, maxZ, floorY } })
      },
      undefined,
      (err) => reject(err),
    )
  })
}
