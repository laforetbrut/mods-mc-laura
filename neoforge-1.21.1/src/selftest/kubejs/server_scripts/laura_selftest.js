// Used by the Laura self tests (compat_kubejs). Shows what scripts can do with Laura.
Laura.addGift({ match: 'minecraft:emerald_block', affection: 60, fun: 30, tier: 'AMAZING', returnChance: 0.5 })
Laura.addFavoriteFood('minecraft:baked_potato')
Laura.addChat('en_us', 'kubejs_test', 'kubejs test phrase', 'Scripts are fun!')
// Never cancelled: the self test checks that her answer to this one is heard.
Laura.addChat('en_us', 'kubejs_plain', 'kubejs plain phrase', 'Plain answers work')
Laura.addLine('en_us', 'hug', 'A hug from a script!')
Laura.addMeal({ id: 'kubejs_bread', result: 'minecraft:bread', count: 2, ingredients: { 'minecraft:wheat': 2 } })

// Lets the self test put a plain item in one of her Curios slots (compat_curios_death).
ServerEvents.tags('item', event => {
  event.add('curios:ring', 'minecraft:gold_nugget')
})

LauraEvents.summon(event => {
  Laura.addAffection(event.laura, 77)
})
LauraEvents.chat(event => {
  if (event.detail == 'kubejs_test') {
    Laura.emote(event.laura, 'celebrate')
    // Stops her default answer ('Scripts are fun!' is never said).
    event.cancel()
  }
})
