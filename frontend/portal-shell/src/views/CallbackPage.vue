<script setup lang="ts">
import userManager from "../services/authService.ts";
import { onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

onMounted(() => {
  userManager.signinRedirectCallback()
      .then(() => {
        router.push("/");
      })
      .catch((error) => {
        console.log(error);
      });
})
</script>

<template>
 <div>Processing Login...</div>
</template>

<style scoped>

</style>