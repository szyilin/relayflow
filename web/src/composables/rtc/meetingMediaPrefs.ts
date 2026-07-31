import { ref } from 'vue'

/** Session prefs from pre-join → room (not persisted across reloads). */
export const meetingAudioDeviceId = ref<string | undefined>()
export const meetingVideoDeviceId = ref<string | undefined>()
export const meetingAudioOutputId = ref<string | undefined>()
/** 0–1 remote playback volume */
export const meetingSpeakerVolume = ref(0.8)

export function setMeetingMediaPrefs(opts: {
  audioDeviceId?: string
  videoDeviceId?: string
  audioOutputId?: string
  speakerVolume?: number
}) {
  meetingAudioDeviceId.value = opts.audioDeviceId || undefined
  meetingVideoDeviceId.value = opts.videoDeviceId || undefined
  meetingAudioOutputId.value = opts.audioOutputId || undefined
  if (typeof opts.speakerVolume === 'number' && Number.isFinite(opts.speakerVolume)) {
    meetingSpeakerVolume.value = Math.min(1, Math.max(0, opts.speakerVolume))
  }
}
