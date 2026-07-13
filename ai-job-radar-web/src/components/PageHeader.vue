<script setup>
defineProps({ eyebrow: String, title: String, subtitle: String })
</script>
<template><header class="page-header"><div><span class="eyebrow">{{ eyebrow }}</span><h1>{{ title }}</h1><p>{{ subtitle }}</p></div><slot /></header></template>
