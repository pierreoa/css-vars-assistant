package cssvarsassistant.documentation

data class ResolutionInfo(val original: String, val resolved: String, val steps: List<String> = emptyList())