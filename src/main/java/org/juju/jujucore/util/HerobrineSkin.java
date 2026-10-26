package org.juju.jujucore.util;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;

import java.util.UUID;

// classic herobrine skin (steve with white eyes) from the account "theHerobrine", signed by mojang.
// dont use the "Herobrine" account, that one has a cat skin lol
public class HerobrineSkin {

    private static final UUID ID = UUID.fromString("5bcc02a7-d519-4c6b-8373-51d98eb8b99b");
    private static final String NAME = "theHerobrine";
    private static final String TEXTURES = "ewogICJ0aW1lc3RhbXAiIDogMTc5MjUxNDA3MTk3MSwKICAicHJvZmlsZUlkIiA6ICI1YmNjMDJhN2Q1MTk0YzZiODM3MzUxZDk4ZWI4Yjk5YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVIZXJvYnJpbmUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFhNWE0MTNjNTVlZGZlYTg1MDUzMDY3NzExNmI1ZWZiOGZhZmE2NDI0MGRiODljOTg4NDc2NzI1OTcyNTQxOCIKICAgIH0KICB9Cn0=";
    private static final String SIGNATURE = "qFGGQCEte9BldfFIhsCVglv3hrtMOjdqjY4sk4RUKdNScy93bY/M4G5/Jf3Ia6QSmKWprS2q5GWb5CmpEG12SksM9z2HGOOAdSaDZYHAFnXvoobJTFN2kyFxpe713lgyNI8jXFTH5yLrbeEheLMXDPL5req73ZTD6lLqfrkRupGyLXuLE0kChK6WNtsQqfyI54dnMzP0gpLgG760rrPoDe7UX/8IL6b4O44XJ5NpCHRUhin7lZd5hcynRSu8dfADFdPCUukU0GG0etEmFDM7KcHCpb8cyprnop9KELedd53sHRBTm0CtnlXKj51Rafbw8vRvY+l7O+jdl2dC+kPxTUEkzT6cxiEid71pLp1/zAFqfu4FIC1YrxlC6PY7SGZ/hiwDlc3hb6fqwGytckIllvcswpVnXj8pYgCfQc5Sty8CKe5Eyc4xitOgb4uRoYg2BIXzm+52VlA7osuNse33fvQ+D96Y2trjSqrq/26FTBmm2ArBzYAQooogPWP20Ek86wRRlMey3/r32lWt40/DiUfLLhQ4QOfml/j/wQAb0CgMPeM7qB0ajaQzNI5wH43b6s+gTefL8O4VghXmGFc3rRFOJdaoGwDzJch+OsT5RI0XjRkVMC7pJRvMLgnC/lz3jpaoWuT0WpOX/MOOmBKEIwugMQT5wxAwYuIakLr/zBA=";

    private HerobrineSkin() {
    }

    public static ResolvableProfile profile() {
        return ResolvableProfile.resolvableProfile()
                .name(NAME)
                .uuid(ID)
                .addProperty(new ProfileProperty("textures", TEXTURES, SIGNATURE))
                .build();
    }
}
