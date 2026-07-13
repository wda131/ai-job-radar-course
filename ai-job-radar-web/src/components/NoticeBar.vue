<script setup>defineProps({ message: String, type: { type: String, default: 'ok' } })</script>
<template><Transition name="notice"><div v-if="message" class="notice" :class="type">{{ message }}</div></Transition></template>
