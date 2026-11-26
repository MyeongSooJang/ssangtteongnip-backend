'use strict'
// guard.cjs(검사)와 mark-test.cjs(기록)가 함께 쓰는 "테스트 통과 기록" 도구.
// 기록은 커밋이 아니라 소스 파일 내용의 지문(fingerprint)에 묶는다. 그래서 테스트 → 커밋 → push 순서에서는 같은 기록이 계속 유효하고,
// 소스를 한 글자라도 고치면 지문이 달라져 다시 테스트해야 한다. 문서·훅만 바뀌면 지문이 그대로라 테스트를 다시 돌리지 않아도 된다.
const crypto = require('crypto')
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const MARKER_FILE = 'test-pass.json'
const SOURCE_PATH = /^(src\/|gradle\/|build\.gradle|settings\.gradle|gradle\.properties$)/

const isSourcePath = file => SOURCE_PATH.test(file)

function sourceFingerprint(root) {
  const listed = execFileSync('git', ['ls-files', '-z', '--cached', '--others', '--exclude-standard'], { cwd: root, encoding: 'utf8' })
  const files = [...new Set(listed.split('\0').filter(Boolean))].filter(isSourcePath).sort()
  const hash = crypto.createHash('sha256')
  for (const file of files) {
    let content
    try {
      content = fs.readFileSync(path.join(root, file))
    } catch {
      continue // 지운 파일은 지문에서 빠진다
    }
    hash.update(file).update('\0').update(content).update('\0')
  }
  return hash.digest('hex')
}

// 명령 문자열에서 heredoc 본문과 따옴표 속 글을 지운다(커밋 메시지에 적힌 "./gradlew test" 같은 글에 반응하지 않게).
const stripCommand = command =>
  command
    .replace(/<<-?\s*(['"]?)(\w+)\1[\s\S]*?\n\s*\2(?=\s|$)/g, '')
    .replace(/"(?:[^"\\]|\\.)*"|'[^']*'/g, '""')

// 전체 테스트를 돌린 명령만 인정한다. 일부만 돌리거나(--tests), 테스트를 빼거나(-x test), 실행하지 않는(--dry-run) 경우는 제외한다.
function isFullTestRun(command) {
  const code = stripCommand(command)
  const gradle = code.match(/(?:^|[\s;&|])(?:\.[\\/])?gradlew(?:\.bat)?(?=\s|$)([^\n;&|]*)/)
  if (!gradle) return false
  const args = gradle[1]
  if (/(^|\s)(-x|--exclude-task)\s+\S*test\b/i.test(args)) return false
  if (/--tests\b|(^|\s)(--dry-run|-m)(\s|$)/.test(args)) return false
  return args.split(/\s+/).some(token => ['test', 'check', 'build'].includes(token))
}

const markerPath = stateDir => path.join(stateDir, MARKER_FILE)

module.exports = { isSourcePath, sourceFingerprint, isFullTestRun, markerPath, stripCommand }
