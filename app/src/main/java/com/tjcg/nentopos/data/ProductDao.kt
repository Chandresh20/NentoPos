package com.tjcg.nentopos.data

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface ProductDao {

    @Insert(onConflict = REPLACE)
    fun insertAllMenuData(allMenus: List<MenuData>)

    @Insert(onConflict = REPLACE)
    fun insertAllCategoryData(allCategories: List<CategoryData>)

    @Insert(onConflict = REPLACE)
    fun insertAllProductsData(allProducts: List<ProductData>)

    @Insert(onConflict = REPLACE)
    fun insertAllProductVariants(variants : List<ProductVariants>)

    @Insert(onConflict = REPLACE)
    fun insertAllProductAddOns(addOns: List<ProductAddOns>)

    @Insert(onConflict = REPLACE)
    fun insertAllTaxesData(allTaxes: List<ProductTax>)

    @Insert(onConflict = REPLACE)
    fun insertAllModifiers(allModifiers: List<ProductModifier>)

    @Insert(onConflict = REPLACE)
    fun insertAllSubModifiers(allSubModifiers : List<ProductSubModifier>)

    @Insert(onConflict = REPLACE)
    fun insertOneProductData(productData: ProductData)

    @Insert(onConflict = REPLACE)
    fun insertAllDiscounts(allDiscounts : List<DiscountData>)

    @Query("select * from Menus where outletId=:outletId")
    fun getAllMenuData(outletId: Int?) : List<MenuData>?

    @Update()
    fun updateMenuData(menuData: MenuData)

    @Query("select menuName from Menus where menuId=:id")
    fun getMenuName(id:Int) : String?

    @Query("select * from Categories where categoryId=:catId")
    fun getAllProductFromCategory(catId: Int) : CategoryData?

    @Query("select * from Products where productId=:id")
    fun getProductData(id: Int) : ProductData?

    @Query("select * from Variants where variantId=:vId")
    fun getOneVariantData(vId : Int) : ProductVariants?

    @Query("select * from ProductModifiers where modifierId=:mId")
    fun getOneModifierData(mId : Int) : ProductModifier?

    @Query("select * from ProductSubModifiers where subModifierId=:sId")
    fun getOneSubModifierDetails(sId : Int) : ProductSubModifier?

    @Query("select * from ProductAddOns where addOnId=:adId")
    fun getOneAddOnDetails(adId: Int) : ProductAddOns?

    @Query("select * from ProductTaxes")
    fun getAllTaxesData() : List<ProductTax>?

    @Query("select * from ProductTaxes where id=:tId")
    fun getOneTaxData(tId : Int) : ProductTax?

    @Query("select * from Discounts")
    fun getAllDiscounts() : List<DiscountData>?

    @Query("delete from Menus")
    fun deleteAllMenus()

    @Query("delete from Categories")
    fun deleteAllCategories()

    @Query("delete from Products")
    fun deleteAllProducts()

    @Query("delete from ProductTaxes")
    fun deleteAllTaxes()

    @Query("delete from variants")
    fun deleteAllVariants()

    @Query("delete from ProductAddOns")
    fun deleteAllAddOns()

    @Query("delete from ProductModifiers")
    fun deleteAllModifiers()

    @Query("delete from ProductSubModifiers")
    fun deleteAllSubModifiers()

    @Query("delete from Discounts")
    fun deleteAllDiscountData()
}