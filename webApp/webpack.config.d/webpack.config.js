// OPFSの高速アクセス(SharedArrayBuffer)に必要なcross-origin isolationを有効化する。
// これが無いとOPFSでの永続化が動かない（crossOriginIsolatedがfalseになる）
;(function(config) {
  // devServerはbrowserRun系タスクのときだけ存在する（webpack単体タスクではundefined）
  if (config.devServer) {
    config.devServer.headers = [
        { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { key: 'Cross-Origin-Embedder-Policy', value: 'require-corp' }
    ]
  }
})(config);
