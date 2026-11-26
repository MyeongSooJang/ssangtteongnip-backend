#!/usr/bin/env node
'use strict'
// PostToolUse(Bash|PowerShell) 훅. `./gradlew test`(또는 check·build)가 성공하면 그때의 소스 지문을 .claude/.state/test-pass.json에 기록한다.
// guard.cjs는 소스가 바뀐 커밋·push·PR 생성 전에 이 기록이 현재 소스와 같은지 확인한다.
//  - 실패하면 기존 기록을 지운다(실패한 상태에서 이전 통과 기록으로 통과하지 못하게 한다).
//  - 성공·실패는 출력의 "BUILD SUCCESSFUL" / "BUILD FAILED"로 판단한다. 둘 다 없으면(중단·잘림) 기록을 건드리지 않는다.
//  - `cd`로 다른 폴더에서 실행한 명령은 이 저장소의 테스트인지 알 수 없어 기록하지 않는다(막는 쪽이 안전하다).
// 한계: 사용자가 직접 입력하는 `! 명령`은 훅을 거치지 않아 기록되지 않는다. 그때는 에이전트가 테스트를 다시 돌리면 된다.
const fs = require('fs')
const path = require('path')
const { isFullTestRun, sourceFingerprint, markerPath } = require('./test-marker.cjs')

const ROOT = path.resolve(__dirname, '..', '..')
const STATE_DIR = path.join(ROOT, '.claude', '.state')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8'))
} catch {
  process.exit(0)
}

const command = input.tool_input && input.tool_input.command
if (typeof command !== 'string' || !isFullTestRun(command) || /(^|[\s;&|])cd\s/.test(command)) process.exit(0)

const output = JSON.stringify(input.tool_response || '')
const file = markerPath(STATE_DIR)

if (/BUILD SUCCESSFUL/.test(output)) {
  fs.mkdirSync(STATE_DIR, { recursive: true })
  fs.writeFileSync(file, JSON.stringify({ fingerprint: sourceFingerprint(ROOT), session_id: input.session_id, at: new Date().toISOString() }, null, 2))
} else if (/BUILD FAILED/.test(output)) {
  fs.rmSync(file, { force: true })
}
