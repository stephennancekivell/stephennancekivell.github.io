---
layout: home
---

<div class="home">
  
  <ul class="post-list">
    {% for post in site.posts %}
      <li>
        {% assign date_format = site.minima.date_format | default: "%b %-d, %Y" %}
        <span class="post-meta">{{ post.date | date: date_format }}</span>

        <h2>
          <a class="post-link" href="{{ post.url | relative_url }}">{{ post.title | escape }}</a>
        </h2>

        {% if post.summary %}
          <p>{{ post.summary }}</p>
        {% endif %}

      </li>
    {% endfor %}
  </ul>
  
</div>
