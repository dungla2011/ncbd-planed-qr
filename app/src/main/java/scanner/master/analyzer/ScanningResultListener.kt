package scanner.master.analyzer

interface ScanningResultListener {
    fun onScanned(result: String)
}