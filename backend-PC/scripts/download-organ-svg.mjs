import fs from 'fs'
import https from 'https'
import { dirname, join } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const outDir = join(__dirname, '../public/images')
fs.mkdirSync(outDir, { recursive: true })

const files = [
  [
    'https://upload.wikimedia.org/wikipedia/commons/8/8d/Man_shadow_anatomy.svg',
    'organ-front.svg',
  ],
]

function get(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`HTTP ${res.statusCode}`))
          return
        }
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')))
      })
      .on('error', reject)
  })
}

for (const [url, name] of files) {
  try {
    const text = await get(url)
    if (!text.includes('<svg')) throw new Error('not svg')
    fs.writeFileSync(join(outDir, name), text, 'utf8')
    console.log('OK', name, text.length)
  } catch (e) {
    console.error('FAIL', name, e.message)
  }
}
