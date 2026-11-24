package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MintJade

enum class LegalDocumentType(val titleFa: String) {
    TERMS("قوانین و مقررات"),
    PRIVACY("حریم خصوصی")
}

private data class LegalSection(
    val heading: String,
    val body: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalPagesScreen(
    initialTab: LegalDocumentType = LegalDocumentType.TERMS,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by remember(initialTab) {
        mutableIntStateOf(if (initialTab == LegalDocumentType.TERMS) 0 else 1)
    }

    val termsSections = remember {
        listOf(
            LegalSection(
                heading = "۱. معرفی سرویس eSkyTell",
                body = "اپلیکیشن eSkyTell یک پل ارتباطی امن برای مدیریت و همگام‌سازی تماس‌ها، مخاطبین و پیامک‌ها بین گوشی دوم (اندروید) و دستگاه اصلی (آیفون یا گوشی دیگر) از طریق ارتباط مستقیم بلوتوث است. استفاده از اپلیکیشن به معنای پذیرش کامل این قوانین است."
            ),
            LegalSection(
                heading = "۲. مسئولیت استفاده قانونی",
                body = "کاربر متعهد می‌شود از قابلیت‌های همگام‌سازی و انتقال بلوتوثی صرفاً برای دستگاه‌ها و سیم‌کارت‌های متعلق به خود و در چارچوب قوانین جاری استفاده نماید. هرگونه استفاده غیرمجاز برای شنود یا دسترسی بدون رضایت به داده‌های اشخاص ثالث ممنوع است."
            ),
            LegalSection(
                heading = "۳. اشتراک‌ها، تعرفه تومان و پرداخت‌ها",
                body = "نسخه پایه ممکن است طبق سیاست مدیریت به‌صورت رایگان یا با دوره تست رایگان ارائه شود. در حالت اشتراکی، مبالغ پلن‌های ماهانه و سالانه به تومان در صفحه اشتراک نمایش داده می‌شود. پس از واریز به شماره کارت یا کیف پول رمزارز اعلام‌شده و ارسال کد پیگیری یا تصویر فیش، بررسی توسط مدیریت انجام شده و اشتراک به صورت خودکار روی حساب فعال می‌شود."
            ),
            LegalSection(
                heading = "۴. تمدید، لغو و پشتیبانی پرداخت",
                body = "تمدید اشتراک‌ها به‌صورت دستی و بدون کسر خودکار بانکی انجام می‌شود. پیش از اتمام مهلت اشتراک، اعلان یادآوری تمدید ارسال می‌گردد. در صورت بروز مغایرت در فیش یا خطا در انتقال، کاربر می‌تواند از طریق ایمیل پشتیبانی درخواست بررسی مجدد ثبت کند."
            ),
            LegalSection(
                heading = "۵. به‌روزرسانی و تغییرات",
                body = "توسعه‌دهنده مجاز است قابلیت‌های جدید، بهبودهای امنیتی یا تغییرات تعرفه و شرایط استفاده را در نسخه‌های بعدی اعمال کرده و از طریق بخش اطلاعیه‌های برنامه به کاربران اطلاع‌رسانی کند."
            )
        )
    }

    val privacySections = remember {
        listOf(
            LegalSection(
                heading = "۱. داده‌های احراز هویت و پروفایل",
                body = "برای ورود امن به برنامه از سرویس احراز هویت گوگل (Google Sign-In / Credential Manager) استفاده می‌شود. تنها شناسه کاربری، نام نمایشی، ایمیل و برچسب‌های سفارشی سیم‌کارت (SIM 1 / SIM 2) برای همگام‌سازی تنظیمات حساب در سرور ابری ذخیره می‌گردند."
            ),
            LegalSection(
                heading = "۲. حریم خصوصی پیامک‌ها، تماس‌ها و مخاطبین",
                body = "اطلاعات تماس، مخاطبین و پیامک‌های ثبت‌شده در پل بلوتوثی روی حافظه محلی دستگاه شما نگهداری می‌شوند و بین گوشی اول و دوم با پین جفت‌سازی و رمزنگاری AES-GCM منتقل می‌گردند. eSkyTell این محتواهای شخصی را به سرورهای تبلیغاتی ارسال یا به اشخاص ثالث نمی‌فروشد."
            ),
            LegalSection(
                heading = "۳. تصاویر فیش واریزی و مجوزهای سیستم",
                body = "برای انتخاب تصویر فیش واریزی از گالری از انتخابگر استاندارد بدون‌مجوز اندروید (Photo Picker) استفاده می‌شود و هیچ دسترسی گسترده‌ای به کل فایل‌ها یا گالری شما دریافت نمی‌گردد. تصویر فیش صرفاً برای تایید پرداخت توسط مدیر کل نگهداری و بررسی می‌شود."
            ),
            LegalSection(
                heading = "۴. مجوزهای بلوتوث و اعلان‌ها",
                body = "دسترسی‌های بلوتوث (اسکن، اتصال و تبلیغ دستگاه) فقط برای کشف گوشی دوم و برقراری پل ارتباطی استفاده می‌شوند. مجوز اعلان (POST_NOTIFICATIONS) نیز برای اطلاع‌رسانی تماس/پیامک پل و یادآوری سررسید تمدید اشتراک کاربرد دارد."
            ),
            LegalSection(
                heading = "۵. حقوق کاربر و حذف اطلاعات",
                body = "کاربر هر زمان می‌تواند تاریخچه انتقال‌های محلی را از تنظیمات پاک کرده، اعلان‌های یادآوری را غیرفعال کند یا برای حذف کامل داده‌های ابری و درخواست‌های پرداخت با پشتیبانی به نشانی sepehrtixzor1@gmail.com مکاتبه نماید."
            )
        )
    }

    val currentSections = if (selectedTab == 0) termsSections else privacySections

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Rounded.Gavel else Icons.Rounded.PrivacyTip,
                            contentDescription = null,
                            tint = MintJade
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("اطلاعات حقوقی eSkyTell", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("legal_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(LegalDocumentType.TERMS.titleFa) },
                        icon = { Icon(Icons.Rounded.Gavel, contentDescription = null) },
                        modifier = Modifier.testTag("legal_tab_terms")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(LegalDocumentType.PRIVACY.titleFa) },
                        icon = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                        modifier = Modifier.testTag("legal_tab_privacy")
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            color = MintJade.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MintJade.copy(alpha = 0.4f)),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MintJade, modifier = Modifier.size(24.dp))
                                Column {
                                    Text(
                                        text = if (selectedTab == 0) "شرایط و ضوابط استفاده از خدمات eSkyTell"
                                        else "تعهدنامه حفظ حریم خصوصی و امنیت داده‌ها",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "آخرین به‌روزرسانی: آبان ۱۴۰۵ • مطابق با استانداردهای امنیتی گوگل پلی",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ChampagneGold
                                    )
                                }
                            }
                        }
                    }

                    items(currentSections) { section ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = section.heading,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MintJade
                                )
                                Text(
                                    text = section.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
