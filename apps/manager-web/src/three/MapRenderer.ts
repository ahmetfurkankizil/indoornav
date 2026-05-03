import type { Dxf } from 'dxf-parser'
import { renderGlbTopDown, type GlbRenderResult } from './GlbRenderer'

export type { GlbRenderResult as MapRenderResult }
export type { RenderBounds } from './GlbRenderer'

/**
 * Render a map file from a Blob (for local/IndexedDB-sourced files).
 * mimeType is used to determine the format; falls back to blob.type.
 */
export async function renderMapBlob(blob: Blob, mimeType?: string): Promise<GlbRenderResult> {
  const ct = ((mimeType ?? blob.type) ?? '').toLowerCase()
  if (ct.includes('svg') || ct.includes('png') || ct.includes('jpeg') || ct.includes('webp') || ct.includes('image')) {
    return render2DImageFromBlob(blob)
  }
  if (ct.includes('dxf')) {
    return renderDxfFromText(await blob.text())
  }
  const objUrl = URL.createObjectURL(blob)
  try { return await renderGlbTopDown(objUrl) } finally { URL.revokeObjectURL(objUrl) }
}

/**
 * Fetch the map file once, detect format from Content-Type, and render to a
 * top-down 1024×1024 image. Supports GLB, SVG, PNG, and DXF.
 */
export async function renderMapFile(
  mapUrl: string,
  token?: string,
): Promise<GlbRenderResult> {
  const headers: HeadersInit = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(mapUrl, { headers })
  if (!res.ok) throw new Error(`Server returned ${res.status} for map file`)

  const ct = (res.headers.get('Content-Type') ?? '').toLowerCase()

  if (ct.includes('svg')) {
    return render2DImageFromBlob(await res.blob())
  }
  if (ct.includes('png') || ct.includes('jpeg') || ct.includes('webp')) {
    return render2DImageFromBlob(await res.blob())
  }
  if (ct.includes('dxf')) {
    return renderDxfFromText(await res.text())
  }

  // GLB / octet-stream: hand the already-fetched bytes to Three.js via a blob URL
  const glbBlob = await res.blob()
  const glbObjectUrl = URL.createObjectURL(glbBlob)
  try {
    return await renderGlbTopDown(glbObjectUrl)
  } finally {
    URL.revokeObjectURL(glbObjectUrl)
  }
}

// ── 2-D image (SVG / PNG) ──────────────────────────────────────────────────────

function render2DImageFromBlob(blob: Blob): Promise<GlbRenderResult> {
  const objectUrl = URL.createObjectURL(blob)
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      try {
        const SIZE = 1024
        const canvas = document.createElement('canvas')
        canvas.width = SIZE
        canvas.height = SIZE
        const ctx = canvas.getContext('2d')!
        ctx.fillStyle = '#f0f0f0'
        ctx.fillRect(0, 0, SIZE, SIZE)

        // SVGs without explicit width/height report 0 — fall back to full canvas
        const iw = img.naturalWidth  || SIZE
        const ih = img.naturalHeight || SIZE
        const scale = Math.min(SIZE / iw, SIZE / ih)
        const w = iw * scale
        const h = ih * scale
        ctx.drawImage(img, (SIZE - w) / 2, (SIZE - h) / 2, w, h)

        URL.revokeObjectURL(objectUrl)
        resolve({
          imageDataUrl: canvas.toDataURL('image/png'),
          bounds: { minX: 0, maxX: 1, minZ: 0, maxZ: 1, floorY: 0 },
        })
      } catch (e) {
        URL.revokeObjectURL(objectUrl)
        reject(e)
      }
    }
    img.onerror = e => { URL.revokeObjectURL(objectUrl); reject(e) }
    img.src = objectUrl
  })
}

// ── DXF ────────────────────────────────────────────────────────────────────────

async function renderDxfFromText(text: string): Promise<GlbRenderResult> {
  const { default: DxfParser } = await import('dxf-parser')
  const parser = new DxfParser()
  const dxf = parser.parseSync(text)
  return renderDxfToCanvas(dxf)
}

function renderDxfToCanvas(dxf: Dxf): GlbRenderResult {
  const SIZE = 1024
  const MARGIN = 0.05

  let minX = dxf.header?.$EXTMIN?.x
  let maxX = dxf.header?.$EXTMAX?.x
  let minY = dxf.header?.$EXTMIN?.y
  let maxY = dxf.header?.$EXTMAX?.y

  const entities = dxf.entities ?? []

  if (minX == null || !isFinite(minX) || minX === maxX) {
    minX = Infinity; maxX = -Infinity; minY = Infinity; maxY = -Infinity
    for (const e of entities) {
      const verts = e.vertices ?? (e.startPoint ? [e.startPoint, e.endPoint!] : [])
      for (const v of verts) {
        if (v && isFinite(v.x)) { minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x) }
        if (v && isFinite(v.y)) { minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y) }
      }
      if (e.center && e.radius != null && isFinite(e.radius)) {
        minX = Math.min(minX, e.center.x - e.radius); maxX = Math.max(maxX, e.center.x + e.radius)
        minY = Math.min(minY, e.center.y - e.radius); maxY = Math.max(maxY, e.center.y + e.radius)
      }
    }
  }

  if (minX == null || !isFinite(minX) || minX === maxX) { minX = 0; maxX = 1 }
  if (minY == null || !isFinite(minY) || minY === maxY) { minY = 0; maxY = 1 }

  const rangeX = maxX! - minX!
  const rangeY = maxY! - minY!
  const drawSize = SIZE * (1 - 2 * MARGIN)
  const scaleR = Math.min(drawSize / rangeX, drawSize / rangeY)

  const toX = (x: number) => ((x - minX!) / rangeX) * drawSize + SIZE * MARGIN
  const toY = (y: number) => SIZE - (((y - minY!) / rangeY) * drawSize + SIZE * MARGIN)

  const canvas = document.createElement('canvas')
  canvas.width = SIZE
  canvas.height = SIZE
  const ctx = canvas.getContext('2d')!
  ctx.fillStyle = '#f8f9fa'
  ctx.fillRect(0, 0, SIZE, SIZE)
  ctx.strokeStyle = '#1f2937'
  ctx.lineWidth = 1
  ctx.beginPath()

  for (const e of entities) {
    switch (e.type) {
      case 'LINE': {
        const v = e.vertices ?? []
        if (v.length >= 2) { ctx.moveTo(toX(v[0].x), toY(v[0].y)); ctx.lineTo(toX(v[1].x), toY(v[1].y)) }
        break
      }
      case 'LWPOLYLINE':
      case 'POLYLINE': {
        const v = e.vertices ?? []
        if (!v.length) break
        ctx.moveTo(toX(v[0].x), toY(v[0].y))
        for (let i = 1; i < v.length; i++) ctx.lineTo(toX(v[i].x), toY(v[i].y))
        if (e.closed) ctx.closePath()
        break
      }
      case 'ARC': {
        if (!e.center || e.radius == null || !isFinite(e.radius)) break
        const cx = toX(e.center.x), cy = toY(e.center.y), r = e.radius * scaleR
        const start = e.startAngle ?? 0
        const end   = e.endAngle   ?? 360
        const span  = end > start ? end - start : 360 - start + end
        const steps = Math.max(16, Math.round(span / 5))
        for (let i = 0; i <= steps; i++) {
          const rad = (start + span * i / steps) * Math.PI / 180
          const px = cx + r * Math.cos(rad)
          const py = cy - r * Math.sin(rad) // negate sin: DXF Y-up → canvas Y-down
          if (i === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py)
        }
        break
      }
      case 'CIRCLE': {
        if (!e.center || e.radius == null || !isFinite(e.radius)) break
        const cx = toX(e.center.x), cy = toY(e.center.y), r = e.radius * scaleR
        ctx.moveTo(cx + r, cy)
        ctx.arc(cx, cy, r, 0, 2 * Math.PI)
        break
      }
    }
  }

  ctx.stroke()
  return {
    imageDataUrl: canvas.toDataURL('image/png'),
    bounds: { minX: 0, maxX: 1, minZ: 0, maxZ: 1, floorY: 0 },
  }
}
