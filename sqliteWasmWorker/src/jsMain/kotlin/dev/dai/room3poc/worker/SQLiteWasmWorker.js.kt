package dev.dai.room3poc.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual fun createSQLiteWasmWorker() =
    WebWorkerSQLiteDriver(Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)""")))
