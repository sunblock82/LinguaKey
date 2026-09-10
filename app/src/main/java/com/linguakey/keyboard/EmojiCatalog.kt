package com.linguakey.keyboard

object EmojiCatalog {
    data class Category(val name: String, val icon: String, val items: List<String>)
    val categories = listOf(
        Category("표정", "😀", "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🫡 🤭 🫢 🫣 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕".split(" ")),
        Category("손", "👍", "👍 👎 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤝 👏 🙌 🫶 👐 🤲 🙏 ✍️ 💪 🦾 🖕".split(" ")),
        Category("하트", "❤️", "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❤️‍🔥 ❤️‍🩹 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 💌 💋".split(" ")),
        Category("사물", "✨", "✨ ⭐ 🌟 💫 🔥 🎉 🎊 🎁 🏆 🥇 🎯 💡 📌 📍 ✏️ 📝 📚 📖 💻 📱 ⌚ 🎧 📷 🎬 🎮 🧩 🔑 🔒 🔓 💰 💳 📈 📉 ✅ ❌ ⚠️ ❗ ❓ 💯 🚀".split(" ")),
        Category("음식", "🍔", "🍎 🍓 🫐 🍉 🍇 🍌 🍑 🍒 🥑 🥦 🥕 🌽 🍞 🥐 🥨 🧀 🍳 🥞 🍔 🍟 🍕 🌭 🥪 🌮 🍜 🍝 🍣 🍱 🍛 🍚 🍙 🍦 🍰 🎂 🍪 ☕ 🍵 🥤 🍺".split(" ")),
        Category("이동", "✈️", "🚗 🚕 🚌 🚎 🏎️ 🚓 🚑 🚒 🚲 🛵 🏍️ 🚆 🚇 🚄 ✈️ 🛫 🛬 🚀 🚁 ⛵ 🚢 🗺️ 🧳 🏠 🏢 🏫 🏥 🏨 🏖️ 🏕️ 🏔️ 🌍 🌏".split(" "))
    )
}
