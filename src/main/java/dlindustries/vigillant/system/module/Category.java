package dlindustries.vigillant.system.module;

import dlindustries.vigillant.system.utils.EncryptedString;

public enum Category {
	sword(EncryptedString.of("Sword")),
	CRYSTAL(EncryptedString.of("Crystal")),
	pot(EncryptedString.of("Potions")),
	mace(EncryptedString.of("Mace")),
	optimizer(EncryptedString.of("Optimizer")),
	RENDER(EncryptedString.of("Render")),
	CLIENT(EncryptedString.of("Client"));
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
