import path from 'node:path'
import { chromium } from 'playwright'

export async function openBossSession(profileDir) {
  return chromium.launchPersistentContext(path.resolve(profileDir), {
    headless: false,
    viewport: { width: 1365, height: 850 },
    locale: 'zh-CN'
  })
}
