<script setup lang="ts">
import { X, Check, Loader2 } from 'lucide-vue-next'

const { t, locale } = useI18n()
const store = useLeadStore()

const name = ref('')
const agency = ref('')
const email = ref('')
const message = ref('')
const hp = ref('') // honeypot — must stay empty

const loading = ref(false)
const sent = ref(false)
const error = ref('')

const firstField = ref<HTMLInputElement | null>(null)
const card = ref<HTMLElement | null>(null)

function reset() {
  name.value = agency.value = email.value = message.value = hp.value = ''
  loading.value = false
  sent.value = false
  error.value = ''
}

function close() {
  store.hide()
}

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))
const canSubmit = computed(() => name.value.trim() && agency.value.trim() && emailValid.value)

async function submit() {
  error.value = ''
  if (!canSubmit.value) {
    error.value = t('lead.errValidation')
    return
  }
  loading.value = true
  try {
    await $fetch('/api/lead', {
      method: 'POST',
      body: {
        name: name.value.trim(),
        agency: agency.value.trim(),
        email: email.value.trim(),
        message: message.value.trim(),
        plan: store.plan,
        locale: locale.value,
        company_website: hp.value, // honeypot
      },
    })
    sent.value = true
  } catch {
    error.value = t('lead.errSend')
  } finally {
    loading.value = false
  }
}

// Esc to close + focus-trap kept simple (Tab wraps within the card)
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    close()
    return
  }
  if (e.key !== 'Tab' || !card.value) return
  const nodes = card.value.querySelectorAll<HTMLElement>(
    'button, [href], input, textarea, [tabindex]:not([tabindex="-1"])',
  )
  const focusable = Array.from(nodes).filter((n) => !n.hasAttribute('disabled') && n.offsetParent !== null)
  if (!focusable.length) return
  const first = focusable[0]!
  const last = focusable[focusable.length - 1]!
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

watch(
  () => store.open,
  (open) => {
    if (typeof document === 'undefined') return
    if (open) {
      reset()
      document.documentElement.style.overflow = 'hidden'
      window.addEventListener('keydown', onKeydown)
      nextTick(() => firstField.value?.focus())
    } else {
      document.documentElement.style.overflow = ''
      window.removeEventListener('keydown', onKeydown)
    }
  },
)

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.documentElement.style.overflow = ''
    window.removeEventListener('keydown', onKeydown)
  }
})
</script>

<template>
  <ClientOnly>
    <Teleport to="body">
      <Transition name="lead">
        <div
          v-if="store.open"
          class="lead"
          role="dialog"
          aria-modal="true"
          :aria-label="t('lead.title')"
          @mousedown.self="close"
        >
          <div ref="card" class="lead__card">
            <button class="lead__x" type="button" :aria-label="t('lead.close')" @click="close">
              <X :size="18" :stroke-width="2" />
            </button>

            <div v-if="!sent" class="lead__body">
              <span v-if="store.plan" class="lead__chip">{{ store.plan }}</span>
              <h2 class="lead__title">{{ t('lead.title') }}</h2>
              <p class="lead__sub">{{ t('lead.subtitle') }}</p>

              <form class="lead__form" novalidate @submit.prevent="submit">
                <!-- honeypot: hidden from users, catches naive bots -->
                <input
                  v-model="hp"
                  class="lead__hp"
                  type="text"
                  name="company_website"
                  tabindex="-1"
                  autocomplete="off"
                  aria-hidden="true"
                />

                <label class="lead__field">
                  <span class="lead__label">{{ t('lead.name') }}</span>
                  <input
                    ref="firstField"
                    v-model="name"
                    class="lead__input"
                    type="text"
                    autocomplete="name"
                    :placeholder="t('lead.namePh')"
                    required
                  />
                </label>

                <label class="lead__field">
                  <span class="lead__label">{{ t('lead.agency') }}</span>
                  <input
                    v-model="agency"
                    class="lead__input"
                    type="text"
                    autocomplete="organization"
                    :placeholder="t('lead.agencyPh')"
                    required
                  />
                </label>

                <label class="lead__field">
                  <span class="lead__label">{{ t('lead.email') }}</span>
                  <input
                    v-model="email"
                    class="lead__input"
                    type="email"
                    autocomplete="email"
                    :placeholder="t('lead.emailPh')"
                    required
                  />
                </label>

                <label class="lead__field">
                  <span class="lead__label">{{ t('lead.message') }}</span>
                  <textarea
                    v-model="message"
                    class="lead__input lead__textarea"
                    rows="3"
                    :placeholder="t('lead.messagePh')"
                  />
                </label>

                <p v-if="error" class="lead__err" role="alert">{{ error }}</p>

                <button type="submit" class="cta cta--primary lead__submit" :disabled="loading">
                  <Loader2 v-if="loading" :size="17" :stroke-width="2.25" class="lead__spin" />
                  <span>{{ loading ? t('lead.sending') : t('lead.submit') }}</span>
                </button>
                <p class="lead__fine">{{ t('lead.fine') }}</p>
              </form>
            </div>

            <div v-else class="lead__done">
              <div class="lead__doneMark"><Check :size="26" :stroke-width="2.5" /></div>
              <h2 class="lead__title">{{ t('lead.successTitle') }}</h2>
              <p class="lead__sub">{{ t('lead.successBody') }}</p>
              <button type="button" class="cta cta--ghost lead__submit" @click="close">
                {{ t('lead.close') }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </ClientOnly>
</template>

<style scoped>
.lead {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.68);
  backdrop-filter: saturate(140%) blur(6px);
  -webkit-backdrop-filter: saturate(140%) blur(6px);
}
.lead__card {
  position: relative;
  width: 100%;
  max-width: 440px;
  max-height: calc(100dvh - 40px);
  overflow-y: auto;
  padding: 30px;
  border: 1px solid var(--hairline-strong);
  border-radius: 22px;
  background: linear-gradient(180deg, var(--surface-2), var(--surface));
  box-shadow: 0 40px 120px -40px rgba(0, 0, 0, 0.9), 0 0 0 1px rgba(255, 255, 255, 0.03);
}
.lead__x {
  position: absolute;
  top: 16px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--hairline);
  border-radius: 10px;
  color: var(--text-2);
  background: transparent;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.lead__x:hover { color: var(--text); border-color: var(--hairline-strong); background: rgba(255, 255, 255, 0.04); }
.lead__chip {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  font-family: 'Geist Mono', monospace;
  font-size: 0.6875rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgb(var(--cyan));
  background: rgba(var(--cyan), 0.1);
  border: 1px solid rgba(var(--cyan), 0.28);
}
.lead__title { font-size: 1.5rem; font-weight: 750; letter-spacing: -0.03em; line-height: 1.1; }
.lead__sub { margin-top: 8px; color: var(--text-2); font-size: 0.96875rem; line-height: 1.5; }
.lead__form { margin-top: 22px; display: flex; flex-direction: column; gap: 14px; }
.lead__hp { position: absolute; left: -9999px; width: 1px; height: 1px; opacity: 0; }
.lead__field { display: flex; flex-direction: column; gap: 6px; }
.lead__label { font-size: 0.8125rem; font-weight: 500; color: var(--text-2); letter-spacing: -0.01em; }
.lead__input {
  width: 100%;
  height: 2.75rem;
  padding: 0 0.875rem;
  border: 1px solid var(--hairline-strong);
  border-radius: 0.625rem;
  background: rgba(0, 0, 0, 0.35);
  color: var(--text);
  font-size: 0.9375rem;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.lead__input::placeholder { color: var(--text-3); }
.lead__input:focus {
  outline: none;
  border-color: rgba(var(--cyan), 0.7);
  box-shadow: 0 0 0 3px rgba(var(--cyan), 0.18);
}
.lead__textarea { height: auto; padding: 0.6rem 0.875rem; line-height: 1.5; resize: vertical; min-height: 3.5rem; }
.lead__err {
  margin: -2px 0 0;
  font-size: 0.875rem;
  color: #ff8a8a;
  line-height: 1.4;
}
.lead__submit { width: 100%; margin-top: 4px; }
.lead__submit[disabled] { opacity: 0.6; cursor: default; }
.lead__spin { animation: lead-spin 0.8s linear infinite; }
@keyframes lead-spin { to { transform: rotate(360deg); } }
.lead__fine { margin-top: 2px; text-align: center; font-size: 0.75rem; color: var(--text-3); line-height: 1.45; }
.lead__done { text-align: center; padding: 8px 0; }
.lead__doneMark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin: 6px auto 18px;
  border-radius: 50%;
  color: rgb(var(--cyan));
  background: rgba(var(--cyan), 0.12);
  border: 1px solid rgba(var(--cyan), 0.3);
}
.lead__done .lead__submit { margin-top: 24px; }

/* enter/leave — card lifts in; backdrop fades */
.lead-enter-active, .lead-leave-active { transition: opacity 0.28s ease; }
.lead-enter-active .lead__card, .lead-leave-active .lead__card {
  transition: transform 0.34s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.28s ease;
}
.lead-enter-from, .lead-leave-to { opacity: 0; }
.lead-enter-from .lead__card, .lead-leave-to .lead__card { transform: translateY(14px) scale(0.98); opacity: 0; }
@media (prefers-reduced-motion: reduce) {
  .lead-enter-active, .lead-leave-active,
  .lead-enter-active .lead__card, .lead-leave-active .lead__card { transition: opacity 0.15s linear; }
  .lead-enter-from .lead__card, .lead-leave-to .lead__card { transform: none; }
  .lead__spin { animation: none; }
}
</style>
