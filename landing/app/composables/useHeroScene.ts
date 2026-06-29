import type { Tier } from '~/stores/graphics'

export interface HeroSceneHandle {
  setProgress: (p: number) => void
  stop: () => void
}

interface StartOpts {
  tier: Exclude<Tier, 'static'>
}

const VERT = /* glsl */ `
  void main() { gl_Position = vec4(position.xy, 0.0, 1.0); }
`

/**
 * Dithered transit field — an animated grayscale field resolved entirely through
 * an ordered Bayer dither, with faint flowing "corridors" (abstract routes/signal)
 * and cursor flow. Pure value, no hue: monochrome-native, engineered, 2026-grade.
 */
const FRAG = /* glsl */ `
  precision highp float;
  uniform float uTime;
  uniform vec2 uRes;
  uniform sampler2D uFlow; // cursor flowmap — rg = drag direction, b = wake intensity
  uniform float uProgress; // scroll 0..1
  uniform float uLevels;
  uniform float uCell;
  uniform float uColorMode; // 0 dispersion-only · 1 cyan+dispersion · 2 duotone
  uniform vec3 uAccent;     // electric accent
  uniform float uDisp;      // chromatic dispersion amount (0 = off)
  uniform float uReveal;    // 0→1 boot-up reveal

  float hash(vec2 p){ p = fract(p * vec2(123.34, 345.45)); p += dot(p, p + 34.345); return fract(p.x * p.y); }
  float vnoise(vec2 p){
    vec2 i = floor(p), f = fract(p);
    float a = hash(i), b = hash(i + vec2(1.0,0.0)), c = hash(i + vec2(0.0,1.0)), d = hash(i + vec2(1.0,1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a,b,u.x), mix(c,d,u.x), u.y);
  }
  float fbm(vec2 p){
    float v = 0.0, amp = 0.5;
    for (int i = 0; i < 4; i++){ v += amp * vnoise(p); p *= 2.0; amp *= 0.5; }
    return v;
  }
  float bayer2(vec2 a){ a = floor(a); return fract(a.x * 0.5 + a.y * a.y * 0.75); }
  float bayer4(vec2 a){ return bayer2(0.5 * a) * 0.25 + bayer2(a); }
  float bayer8(vec2 a){ return bayer4(0.5 * a) * 0.25 + bayer2(a); }

  // field value (.x) + moving-signal glow (.y) at a point
  vec2 field(vec2 p){
    float agx = uRes.x / uRes.y;
    float uvx = p.x / agx;

    float tm = uTime;
    float t = tm * 0.05;
    vec2 flow = vec2(tm * 0.11, -tm * 0.028);
    vec2 q = p * 1.7 + flow;
    float f = fbm(q + fbm(q * 0.8 + t));

    float lanes = 0.0;
    for (int i = 0; i < 3; i++){
      float fi = float(i);
      float ly = 0.26 + 0.22 * fi + 0.05 * sin(p.x * 1.3 + t * 1.6 + fi * 2.1);
      float d = abs(p.y - ly);
      float band = smoothstep(0.05, 0.0, d);
      float pulse = pow(0.5 + 0.5 * sin(p.x * 6.0 - tm * (3.2 + fi * 0.9)), 3.0);
      lanes += band * (0.22 + pulse);
    }
    float sweepX = mod(tm * 0.2, agx + 1.4) - 0.7;
    float sweep = exp(-pow((p.x - sweepX) * 2.3, 2.0)) * 0.45;

    // base texture reads as atmosphere across the whole field (lifted floor on the
    // left so it never collapses to dead black); the live signal stays right-weighted
    float baseBias = 0.34 + 0.66 * smoothstep(-0.1, 0.58, uvx);
    float sigBias = smoothstep(-0.05, 0.62, uvx);
    float sig = lanes * 0.42 + sweep * smoothstep(0.05, 0.5, uvx);
    float glow = sig * sigBias;
    float v = f * 0.95 * baseBias + sig * sigBias;
    v = mix(v, smoothstep(0.24, 0.82, v), uProgress * 0.6);
    return vec2(clamp(v, 0.0, 1.0), clamp(glow, 0.0, 1.0));
  }

  void main(){
    vec2 frag = gl_FragCoord.xy;
    vec2 uv = frag / uRes;
    float agx = uRes.x / uRes.y;
    vec2 p = vec2(uv.x * agx, uv.y);
    float thr = bayer8(floor(frag / uCell));

    // cursor flowmap: advect the field along the drag trail + a fading wake that energises it
    vec4 fl = texture2D(uFlow, uv);
    vec2 flowDir = (fl.rg - 0.5) * 2.0;
    float wake = fl.b;
    p += flowDir * 0.7;

    // edge-scaled chromatic dispersion — lens-like: minimal at centre, more at the edges
    float edge = length(uv - 0.5);
    float disp = uDisp * (0.5 + 1.8 * edge);

    vec2 g = field(p);
    float dG = clamp(floor(g.x * uLevels + thr) / uLevels, 0.0, 1.0);
    float dR = dG, dB = dG;
    if (uDisp > 0.0001) {
      dR = clamp(floor(field(p + vec2(disp, 0.0)).x * uLevels + thr) / uLevels, 0.0, 1.0);
      dB = clamp(floor(field(p - vec2(disp, 0.0)).x * uLevels + thr) / uLevels, 0.0, 1.0);
    }
    float dGlow = clamp(floor(g.y * uLevels + thr) / uLevels, 0.0, 1.0);

    vec3 silver = vec3(0.95, 0.96, 1.0);
    vec3 col;
    if (uColorMode < 0.5) {
      col = vec3(dR, dG, dB) * silver;
    } else if (uColorMode < 1.5) {
      col = vec3(dR, dG, dB) * silver;
      col += uAccent * pow(dG, 2.2) * 0.12;      // faint cool cast — ties the whole field to the accent
      col += uAccent * dGlow;                    // cyan halo on the live signal
      col += vec3(1.0) * pow(dGlow, 3.0) * 0.45; // white-hot core on the brightest pulses
    } else {
      vec3 cyan = vec3(0.16, 0.62, 1.0);
      vec3 mag = vec3(1.0, 0.2, 0.7);
      vec3 duo = mix(cyan, mag, clamp(dG, 0.0, 1.0));
      col = mix(vec3(dG) * silver, duo, smoothstep(0.12, 0.72, dG));
    }

    float vig = smoothstep(1.25, 0.35, edge);
    col *= mix(0.62, 1.0, vig);

    // the wake lights up the live signal — cyan flare + hot streak, not a deform
    col += uAccent * wake * 0.85;
    col += vec3(1.0) * wake * wake * 0.35;

    // boot-up reveal: the field resolves from black, a touch earlier on the right
    float rev = smoothstep(0.0, 1.0, uReveal * (1.0 + uv.x * 0.4) - uv.x * 0.15);
    col *= clamp(rev, 0.0, 1.0);

    gl_FragColor = vec4(col, 1.0);
  }
`

// flowmap update — decays the previous frame toward neutral and stamps a brush
// at the cursor whose vector is the drag velocity. Builds a fading, flowing trail.
const FRAG_FLOW = /* glsl */ `
  precision highp float;
  uniform sampler2D uPrev;
  uniform vec2 uResFlow;
  uniform vec2 uMouse;  // 0..1
  uniform vec2 uVel;    // per-frame drag velocity (0..1 space)
  uniform float uAspect;
  void main(){
    vec2 uv = gl_FragCoord.xy / uResFlow;
    vec4 prev = texture2D(uPrev, uv);
    vec2 dir = (prev.rg - 0.5) * 0.945;   // decay direction toward neutral
    float inten = prev.b * 0.93;          // decay wake
    vec2 p = vec2(uv.x * uAspect, uv.y);
    vec2 m = vec2(uMouse.x * uAspect, uMouse.y);
    float d = distance(p, m);
    float fall = exp(-d * d * 70.0);
    dir += uVel * fall * 3.0;
    inten += fall * length(uVel) * 8.0;
    dir = clamp(dir, -0.5, 0.5);
    inten = clamp(inten, 0.0, 1.0);
    gl_FragColor = vec4(dir + 0.5, inten, 1.0);
  }
`

export async function startHeroScene(canvas: HTMLCanvasElement, opts: StartOpts): Promise<HeroSceneHandle> {
  const THREE = await import('three')
  const high = opts.tier === 'high'

  const parent = canvas.parentElement as HTMLElement
  const W = () => parent.clientWidth || window.innerWidth
  const H = () => parent.clientHeight || window.innerHeight

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: false, powerPreference: 'high-performance' })
  // cap the drawing buffer so we never fry high-DPI / 4K screens (dither hides the lower res)
  function computePR() {
    const longest = Math.max(W(), H(), 1)
    let pr = Math.min(window.devicePixelRatio, high ? 1.6 : 1.1)
    if (longest * pr > 1500) pr = 1500 / longest
    return Math.max(0.65, pr)
  }
  let PR = computePR()
  renderer.setPixelRatio(PR)
  renderer.setSize(W(), H(), false)

  const scene = new THREE.Scene()
  const camera = new THREE.Camera()

  // optional overrides for side-by-side comparison: ?cm=0|1|2 (color mode) & ?ft=seconds (frozen time)
  const params = new URLSearchParams(window.location.search)
  const cmParam = params.get('cm')
  const colorMode = cmParam !== null ? Number(cmParam) : 1 // default: cyan + dispersion
  const ftParam = params.get('ft')
  const fixedTime = ftParam !== null ? Number(ftParam) : null

  const uniforms = {
    uTime: { value: 0 },
    uRes: { value: new THREE.Vector2(W() * PR, H() * PR) },
    uFlow: { value: null as THREE.Texture | null },
    uProgress: { value: 0 },
    uLevels: { value: high ? 4 : 3 },
    uCell: { value: high ? 2 : 3 },
    uColorMode: { value: colorMode },
    uAccent: { value: new THREE.Vector3(0.16, 0.72, 1.0) }, // electric cyan
    uDisp: { value: high ? 0.012 : 0.0 },
    uReveal: { value: fixedTime !== null ? 1 : 0 }, // frozen captures start fully revealed
  }

  const mat = new THREE.ShaderMaterial({ vertexShader: VERT, fragmentShader: FRAG, uniforms, depthTest: false, depthWrite: false })
  const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), mat)
  scene.add(quad)

  // ---- cursor flowmap (ping-pong render targets) ----
  const FLOW_W = 256
  const flowH = () => Math.max(1, Math.round((FLOW_W * H()) / Math.max(W(), 1)))
  const makeRT = () =>
    new THREE.WebGLRenderTarget(FLOW_W, flowH(), {
      minFilter: THREE.LinearFilter,
      magFilter: THREE.LinearFilter,
      format: THREE.RGBAFormat,
      type: THREE.UnsignedByteType,
      depthBuffer: false,
      stencilBuffer: false,
    })
  let rtRead = makeRT()
  let rtWrite = makeRT()

  const flowScene = new THREE.Scene()
  const flowUniforms = {
    uPrev: { value: rtRead.texture },
    uResFlow: { value: new THREE.Vector2(FLOW_W, flowH()) },
    uMouse: { value: new THREE.Vector2(0.7, 0.5) },
    uVel: { value: new THREE.Vector2(0, 0) },
    uAspect: { value: W() / Math.max(H(), 1) },
  }
  const flowMat = new THREE.ShaderMaterial({ vertexShader: VERT, fragmentShader: FRAG_FLOW, uniforms: flowUniforms, depthTest: false, depthWrite: false })
  const flowQuad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), flowMat)
  flowScene.add(flowQuad)

  function clearFlow() {
    const prev = new THREE.Color()
    renderer.getClearColor(prev)
    const prevA = renderer.getClearAlpha()
    renderer.setClearColor(new THREE.Color(0.5, 0.5, 0.0), 1)
    renderer.setRenderTarget(rtRead); renderer.clear()
    renderer.setRenderTarget(rtWrite); renderer.clear()
    renderer.setRenderTarget(null)
    renderer.setClearColor(prev, prevA)
  }
  clearFlow()

  // pointer (raw target; per-frame velocity feeds the flowmap brush)
  const mouse = { x: 0.7, y: 0.5, lx: 0.7, ly: 0.5 }
  function onPointer(e: PointerEvent) {
    mouse.x = e.clientX / window.innerWidth
    mouse.y = 1 - e.clientY / window.innerHeight
  }
  window.addEventListener('pointermove', onPointer, { passive: true })

  function onResize() {
    const w = W()
    const h = H()
    if (w === 0 || h === 0) return
    PR = computePR()
    renderer.setPixelRatio(PR)
    renderer.setSize(w, h, false)
    uniforms.uRes.value.set(w * PR, h * PR)
    rtRead.setSize(FLOW_W, flowH())
    rtWrite.setSize(FLOW_W, flowH())
    flowUniforms.uResFlow.value.set(FLOW_W, flowH())
    flowUniforms.uAspect.value = w / Math.max(h, 1)
    clearFlow()
  }
  const ro = new ResizeObserver(() => onResize())
  ro.observe(parent)
  onResize()

  let scrollProgress = 0
  function setProgress(p: number) {
    scrollProgress = Math.max(0, Math.min(1, p))
  }

  let raf = 0
  let running = true
  let lastDraw = -1
  const minDt = 1000 / 33 // cap ~32fps — the motion is glacial, imperceptible, halves GPU load
  let reveal = fixedTime !== null ? 1 : 0

  function frame(now: number) {
    if (!running) return
    raf = requestAnimationFrame(frame)
    if (lastDraw >= 0 && now - lastDraw < minDt) return
    lastDraw = now
    uniforms.uTime.value = fixedTime !== null ? fixedTime : now * 0.001

    // flowmap pass: stamp drag velocity, advance the fading trail
    const vx = mouse.x - mouse.lx
    const vy = mouse.y - mouse.ly
    mouse.lx = mouse.x
    mouse.ly = mouse.y
    flowUniforms.uMouse.value.set(mouse.x, mouse.y)
    flowUniforms.uVel.value.set(vx, vy)
    flowUniforms.uPrev.value = rtRead.texture
    renderer.setRenderTarget(rtWrite)
    renderer.render(flowScene, camera)
    renderer.setRenderTarget(null)
    const tmp = rtRead
    rtRead = rtWrite
    rtWrite = tmp
    uniforms.uFlow.value = rtRead.texture

    uniforms.uProgress.value += (scrollProgress - uniforms.uProgress.value) * 0.08
    if (fixedTime === null) {
      reveal += (1 - reveal) * 0.05
      uniforms.uReveal.value = reveal
    }
    renderer.render(scene, camera)
  }
  raf = requestAnimationFrame(frame)

  function stop() {
    running = false
    cancelAnimationFrame(raf)
    window.removeEventListener('pointermove', onPointer)
    ro.disconnect()
    quad.geometry.dispose()
    mat.dispose()
    flowQuad.geometry.dispose()
    flowMat.dispose()
    rtRead.dispose()
    rtWrite.dispose()
    renderer.dispose()
  }

  return { setProgress, stop }
}
