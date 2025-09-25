<template>
  <div>
    <h2>Blog Post List</h2>
    <!-- TODO: API 호출로 실제 목록 가져오기 -->
    <p><router-link to="/blog/123">View Post 123</router-link></p>
    <p><router-link to="/blog/456">View Post 456</router-link></p>
    <hr/>
    <router-link to="/blog/write">Write a new Post</router-link>
  </div>
</template>