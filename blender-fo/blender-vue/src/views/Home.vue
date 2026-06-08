<template>
  <div class="home">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="../assets/video/Miku.mp4" type="video/mp4" />
    </video>
    <div class="hero">
      <h1 class="glass-text">镜花水月</h1>
    </div>
    <!-- 波纹特效 Canvas 层 -->
    <canvas ref="rippleCanvas" class="ripple-canvas"></canvas>
  </div>
  
  <div class="scroll-btn">
  <svg class="arrow-icon" aria-hidden="true">
    <use xlink:href="#icon-xiangxia" />
  </svg>
</div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const rippleCanvas = ref(null)
let ctx = null
let ripples = []
let animationId = null

// 波纹类
class Ripple {
  constructor(x, y, speed = 12, fade = 0.01) {
    this.x = x
    this.y = y
    this.radius = 0
    this.maxRadius = 80
    this.opacity = 0.6
    this.lineWidth = 3
    this.speed = speed          // 每帧增加的半径
    this.fade = fade            // 每帧透明度减少量
  }

  update() {
    this.radius += this.speed
    this.opacity -= this.fade
    return this.radius < this.maxRadius && this.opacity > 0
  }

  draw(ctx) {
    ctx.save()
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(255, 255, 255, ${this.opacity})`
    ctx.lineWidth = this.lineWidth
    ctx.stroke()
    ctx.restore()
  }
}

// 生成随机整数
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// 生成 1~3 道波纹，参数为点击坐标
function addRipples(x, y) {
  const count = randomInt(1, 3)   
  for (let i = 0; i < count; i++) {
    const offsetX = (Math.random() - 0.5) * 10
    const offsetY = (Math.random() - 0.5) * 10
    const speed = 0.2 + Math.random() * 0.5;    
    const fade = 0.002 + Math.random() * 0.004; 
    ripples.push(new Ripple(x + offsetX, y + offsetY, speed, fade))
  }
  if (!animationId) {
    animationId = requestAnimationFrame(animateRipples)
  }
}

// 动画循环
function animateRipples() {
  if (!ctx) return
  ctx.clearRect(0, 0, rippleCanvas.value.width, rippleCanvas.value.height)
  let hasAlive = false
  for (let i = 0; i < ripples.length; i++) {
    const alive = ripples[i].update()
    if (alive) {
      hasAlive = true
      ripples[i].draw(ctx)
    } else {
      ripples.splice(i, 1)
      i--
    }
  }
  if (hasAlive) {
    animationId = requestAnimationFrame(animateRipples)
  } else {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

// 调整 canvas 尺寸
function resizeCanvas() {
  if (!rippleCanvas.value) return
  const w = window.innerWidth
  const h = window.innerHeight
  rippleCanvas.value.width = w
  rippleCanvas.value.height = h
  if (ctx) {
    ctx.setTransform(1, 0, 0, 1, 0, 0)
  }
}

// 全局点击监听：生成 1~3 道波纹
function onWindowClick(e) {
  addRipples(e.clientX, e.clientY)
}

onMounted(() => {
  if (rippleCanvas.value) {
    ctx = rippleCanvas.value.getContext('2d')
    resizeCanvas()
    window.addEventListener('resize', resizeCanvas)
    window.addEventListener('click', onWindowClick)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('click', onWindowClick)
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
})
</script>

<style scoped>
/* 容器占满整个视口 */
.home {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 视频背景 */
.bg-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

/* 前景内容 */
.hero {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: white;
  padding: 0 2rem;
}


.glass-text {
  font-size: 300px;
  font-weight: 900;
  letter-spacing: -0.02em;
  margin-top: -200px;
  color: rgba(255, 255, 255, 0.4);
  text-shadow: 8px 10px 6px rgba(0, 0, 0, 0.5);
  transition: all 0.2s;
  -webkit-text-stroke: 0.5px rgba(255, 255, 255, 1);
  user-select: none;
  cursor: default;
}

.glass-text:hover {
  transform: scale(1.05);
  text-shadow: 0 4px 10px rgba(0, 0, 0, 0.4), 0 8px 20px rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.6);
}

/* 圆形半透明按钮，底部居中 */
.scroll-btn {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);   /* 0.2 透明度 */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.3s, transform 0.2s;
  z-index: 200;
  /* 浮动动画 */
  animation: float 2s ease-in-out infinite;
}

/* 箭头图标大小 */
.arrow-icon {
  width: 50px;
  height: 50px;
  fill: white;
  stroke: white;
}

.scroll-btn:hover {
  background: rgba(255, 255, 255, 0.35);
  animation: none;           /* 鼠标悬停时暂停浮动 */
  transform: translateX(-50%) scale(1.05);
  box-shadow: 
    0 0 0 2px rgba(255, 255, 255, 0.7), 
    0 10px 20px rgba(0, 0, 0, 1);
}

/* 上下浮动关键帧 */
@keyframes float {
  0% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(8px);
  }
  100% {
    transform: translateX(-50%) translateY(0);
  }
}

.ripple-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;   
  z-index: 10;
}
</style>