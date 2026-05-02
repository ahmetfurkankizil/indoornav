declare module 'dxf-parser' {
  export interface DxfVertex { x: number; y: number; z?: number }
  export interface DxfEntity {
    type: string
    layer?: string
    vertices?: DxfVertex[]
    startPoint?: DxfVertex
    endPoint?: DxfVertex
    center?: DxfVertex
    radius?: number
    startAngle?: number
    endAngle?: number
    closed?: boolean
  }
  export interface DxfHeader {
    $EXTMIN?: DxfVertex
    $EXTMAX?: DxfVertex
    [key: string]: unknown
  }
  export interface Dxf {
    header: DxfHeader
    entities: DxfEntity[]
    blocks?: Record<string, unknown>
  }
  export default class DxfParser {
    parseSync(text: string): Dxf
    parse(text: string, callback: (err: Error | null, dxf: Dxf) => void): void
  }
}
