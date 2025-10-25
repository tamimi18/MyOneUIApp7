package com.example.oneuiapp;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * FontInfoExtractor - استخراج معلومات الخط من ملفات TTF/OTF
 * 
 * هذا الكلاس يقرأ بيانات الخط مباشرة من الملف باستخدام RandomAccessFile
 * ويستخرج المعلومات المخزنة في جدول "name" داخل ملف الخط.
 * 
 * كيف يعمل:
 * 1. يقرأ header الملف للتأكد من أنه خط صالح
 * 2. يبحث عن جدول "name" ضمن الجداول المتاحة
 * 3. يقرأ سجلات الأسماء (name records) ويستخرج المعلومات
 * 4. يبحث عن جدول "fvar" لمعرفة إذا كان الخط Variable
 */
public class FontInfoExtractor {
    
    /**
     * كلاس داخلي يحتوي على جميع معلومات الخط المستخرجة
     */
    public static class FontInfo {
        public String fullName;        // الاسم الكامل (nameID = 4)
        public String familyName;      // اسم العائلة (nameID = 1)
        public String styleName;       // النمط (nameID = 2)
        public String version;         // الإصدار (nameID = 5)
        public String copyright;       // حقوق النشر (nameID = 0)
        public String designer;        // المصمم (nameID = 9)
        public String description;     // الوصف (nameID = 10)
        public boolean isVariable;     // هل الخط Variable أم Static
        
        // Constructor فارغ
        public FontInfo() {
            fullName = "Unknown";
            familyName = "Unknown";
            styleName = "Regular";
            version = "Unknown";
            copyright = "";
            designer = "";
            description = "";
            isVariable = false;
        }
    }
    
    /**
     * استخراج جميع معلومات الخط من ملف
     * 
     * @param fontFile ملف الخط المراد استخراج معلوماته
     * @return كائن FontInfo يحتوي على جميع المعلومات المستخرجة
     */
    public static FontInfo extractFontInfo(File fontFile) {
        FontInfo info = new FontInfo();
        
        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            // الخطوة 1: التحقق من صحة الملف
            raf.seek(0);
            int sfntVersion = raf.readInt();
            
            // التحقق من أن الملف هو خط صالح (TTF أو OTF)
            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                return info; // ليس خط صالح، نعيد القيم الافتراضية
            }
            
            // الخطوة 2: قراءة عدد الجداول
            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6); // تخطي searchRange, entrySelector, rangeShift
            
            // الخطوة 3: البحث عن جداول "name" و "fvar"
            long nameTableOffset = -1;
            boolean hasFvarTable = false;
            
            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.read(tag);
                String tagName = new String(tag);
                
                raf.skipBytes(4); // تخطي checksum
                long offset = readUInt32(raf);
                readUInt32(raf); // تخطي length
                
                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                } else if ("fvar".equals(tagName)) {
                    hasFvarTable = true;
                }
                
                // إذا وجدنا الجدولين، لا حاجة لمتابعة البحث
                if (nameTableOffset != -1 && hasFvarTable) {
                    break;
                }
            }
            
            // تعيين حالة Variable
            info.isVariable = hasFvarTable;
            
            // الخطوة 4: استخراج المعلومات من جدول name
            if (nameTableOffset == -1) {
                return info; // لا يوجد جدول name، نعيد القيم الافتراضية
            }
            
            raf.seek(nameTableOffset);
            raf.readUnsignedShort(); // format
            int count = raf.readUnsignedShort(); // عدد سجلات الأسماء
            int stringOffset = raf.readUnsignedShort();
            
            // الخطوة 5: قراءة جميع سجلات الأسماء
            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                raf.readUnsignedShort(); // encodingID
                raf.readUnsignedShort(); // languageID
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();
                
                // نقرأ فقط السجلات من منصة Windows (platformID = 3) أو Mac (platformID = 1)
                if (platformID != 3 && platformID != 1) {
                    continue;
                }
                
                // حفظ الموقع الحالي
                long currentPos = raf.getFilePointer();
                
                // الانتقال لموقع النص
                raf.seek(nameTableOffset + stringOffset + offset);
                
                // قراءة النص
                byte[] nameBytes = new byte[length];
                raf.read(nameBytes);
                
                // تحويل البايتات إلى نص
                String name;
                if (platformID == 3) {
                    // Windows uses UTF-16BE
                    name = new String(nameBytes, "UTF-16BE");
                } else {
                    // Mac uses ASCII
                    name = new String(nameBytes, "US-ASCII");
                }
                
                // تعيين المعلومة حسب nameID
                switch (nameID) {
                    case 0: // Copyright
                        if (info.copyright.isEmpty()) {
                            info.copyright = name;
                        }
                        break;
                    case 1: // Font Family
                        if (info.familyName.equals("Unknown")) {
                            info.familyName = name;
                        }
                        break;
                    case 2: // Font Subfamily (Style)
                        if (info.styleName.equals("Regular")) {
                            info.styleName = name;
                        }
                        break;
                    case 4: // Full Name
                        if (info.fullName.equals("Unknown")) {
                            info.fullName = name;
                        }
                        break;
                    case 5: // Version
                        if (info.version.equals("Unknown")) {
                            info.version = name;
                        }
                        break;
                    case 9: // Designer
                        if (info.designer.isEmpty()) {
                            info.designer = name;
                        }
                        break;
                    case 10: // Description
                        if (info.description.isEmpty()) {
                            info.description = name;
                        }
                        break;
                }
                
                // العودة للموقع السابق لقراءة السجل التالي
                raf.seek(currentPos);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return info;
    }
    
    /**
     * قراءة unsigned int (32-bit) من الملف
     * في Java، int هو signed دائماً، لذلك نحوله إلى long ونستخدم mask
     */
    private static long readUInt32(RandomAccessFile raf) throws Exception {
        return ((long) raf.readInt()) & 0xFFFFFFFFL;
    }
        }
