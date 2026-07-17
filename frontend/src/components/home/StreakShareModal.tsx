import { useRef } from 'react'
import { Download, Share2 } from 'lucide-react'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'

export function StreakShareModal({
  open,
  onClose,
  streak,
  longest,
}: {
  open: boolean
  onClose: () => void
  streak: number
  longest: number
  onShared?: () => void
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  const drawCard = (width: number, height: number) => {
    const canvas = canvasRef.current
    if (!canvas) return null
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return null

    const grad = ctx.createLinearGradient(0, 0, width, height)
    grad.addColorStop(0, '#1a1520')
    grad.addColorStop(1, '#2d1f3d')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, width, height)

    ctx.fillStyle = '#f5c542'
    ctx.font = 'bold 96px system-ui, sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(String(streak), width / 2, height * 0.45)

    ctx.fillStyle = '#e8e4dc'
    ctx.font = '600 28px system-ui, sans-serif'
    ctx.fillText('day reading streak', width / 2, height * 0.55)

    ctx.fillStyle = '#9a958c'
    ctx.font = '400 20px system-ui, sans-serif'
    ctx.fillText(`Longest: ${longest} days`, width / 2, height * 0.65)

    ctx.fillStyle = '#c45cff'
    ctx.font = '600 22px system-ui, sans-serif'
    ctx.fillText('WhatIRead', width / 2, height * 0.88)

    return canvas
  }

  const download = (format: 'square' | 'story') => {
    const size = format === 'story' ? { w: 1080, h: 1920 } : { w: 1080, h: 1080 }
    const canvas = drawCard(size.w, size.h)
    if (!canvas) return
    canvas.toBlob((blob) => {
      if (!blob) return
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `whatiread-streak-${streak}.png`
      a.click()
      URL.revokeObjectURL(url)
    })
  }

  const shareNative = async () => {
    drawCard(1080, 1080)
    const canvas = canvasRef.current
    if (!canvas) return
    canvas.toBlob(async (blob) => {
      if (!blob) return
      const file = new File([blob], 'streak.png', { type: 'image/png' })
      if (navigator.share && navigator.canShare?.({ files: [file] })) {
        await navigator.share({
          title: `${streak} day reading streak`,
          text: 'My reading streak on WhatIRead',
          files: [file],
        })
      } else {
        download('square')
      }
    })
  }

  return (
    <Modal open={open} onClose={onClose} title="Share your streak">
      <div className="space-y-4">
        <div className="rounded-2xl bg-gradient-to-br from-[#1a1520] to-[#2d1f3d] p-8 text-center">
          <p className="font-display text-6xl font-bold text-amber-300">{streak}</p>
          <p className="mt-2 text-lg text-ink">day reading streak</p>
          <p className="mt-1 text-sm text-ink-muted">Longest: {longest} days</p>
        </div>
        <canvas ref={canvasRef} className="hidden" aria-hidden />
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" onClick={() => download('square')}>
            <Download className="h-4 w-4" />
            Square (1:1)
          </Button>
          <Button type="button" variant="secondary" onClick={() => download('story')}>
            <Download className="h-4 w-4" />
            Story (9:16)
          </Button>
          <Button type="button" onClick={() => void shareNative()}>
            <Share2 className="h-4 w-4" />
            Share
          </Button>
        </div>
      </div>
    </Modal>
  )
}
