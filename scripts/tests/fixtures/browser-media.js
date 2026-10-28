// Browser-only acceptance fixture: inject before navigating to the meeting room.
// Synthetic canvas/audio tracks exercise real WebRTC without reading a user's devices.
(() => {
  const NativePeer = window.RTCPeerConnection
  window.__smokePeers = []
  window.RTCPeerConnection = new Proxy(NativePeer, {
    construct(target, args) {
      const peer = new target(...args)
      window.__smokePeers.push(peer)
      return peer
    }
  })
  const canvas = document.createElement('canvas')
  canvas.width = 320
  canvas.height = 180
  const context = canvas.getContext('2d')
  let frame = 0
  setInterval(() => {
    context.fillStyle = `hsl(${frame++ % 360} 70% 45%)`
    context.fillRect(0, 0, 320, 180)
    context.fillStyle = 'white'
    context.fillText(`RelayFlow smoke ${frame}`, 20, 90)
  }, 80)
  const audio = new AudioContext()
  const oscillator = audio.createOscillator()
  const destination = audio.createMediaStreamDestination()
  oscillator.frequency.value = 220
  oscillator.connect(destination)
  oscillator.start()
  document.addEventListener('click', () => audio.resume(), { once: true })
  Object.defineProperty(navigator.mediaDevices, 'getUserMedia', {
    configurable: true,
    value: async constraints => new MediaStream([
      ...(constraints.video ? canvas.captureStream(12).getVideoTracks() : []),
      ...(constraints.audio ? destination.stream.getAudioTracks().map(track => track.clone()) : [])
    ])
  })
  window.__smokeMediaStats = async () => {
    const stats = []
    for (const peer of window.__smokePeers) {
      for (const report of (await peer.getStats()).values()) {
        if (['inbound-rtp', 'outbound-rtp'].includes(report.type)) {
          stats.push({ type: report.type, kind: report.kind, bytesReceived: report.bytesReceived,
            bytesSent: report.bytesSent, packetsReceived: report.packetsReceived, framesDecoded: report.framesDecoded })
        }
      }
    }
    return stats
  }
})()
